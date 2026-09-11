package com.ecom.asset;

import com.ecom.shared.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploaderTest {

   private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
   private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
   private static final byte[] GIF = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
   private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

   static class InMemoryStorage implements AssetStorage {
      final Map<String, String> contentTypes = new LinkedHashMap<>();
      final List<String> deleted = new ArrayList<>();
      int failOnPut = -1;
      int puts;

      @Override
      public void put(String key, byte[] content, String contentType) {
         if (puts++ == failOnPut) {
            throw new IllegalStateException("storage down");
         }
         contentTypes.put(key, contentType);
      }

      @Override
      public void delete(String key) {
         deleted.add(key);
         contentTypes.remove(key);
      }

      @Override
      public String publicUrl(String key) {
         return "https://files.example.com/" + key;
      }
   }

   private final InMemoryStorage storage = new InMemoryStorage();
   private final ImageUploader uploader = new ImageUploader(storage, new AssetProperties(1024));

   @Test
   void detectsTheImageTypeFromTheFileItselfNotFromAnyName() {
      assertThat(ImageType.detect(PNG)).contains(ImageType.PNG);
      assertThat(ImageType.detect(JPEG)).contains(ImageType.JPEG);
      assertThat(ImageType.detect(GIF)).contains(ImageType.GIF);
      assertThat(ImageType.detect(WEBP)).contains(ImageType.WEBP);
      assertThat(ImageType.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8))).isEmpty();
      assertThat(ImageType.detect("<?php echo 1;".getBytes(StandardCharsets.UTF_8))).isEmpty();
      assertThat(ImageType.detect(new byte[0])).isEmpty();
      assertThat(ImageType.detect(null)).isEmpty();
      assertThat(ImageType.detect("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII))).as("RIFF but not WebP").isEmpty();
   }

   @Test
   void storesEachImageUnderItsOwnersPrefixAndReturnsThemInTheOrderGiven() {
      List<StoredImage> stored = uploader.upload("user-1", List.of(PNG, JPEG, WEBP));

      assertThat(stored).hasSize(3);
      assertThat(stored.get(0).key()).startsWith("images/user-1/").endsWith(".png");
      assertThat(stored.get(1).key()).startsWith("images/user-1/").endsWith(".jpg");
      assertThat(stored.get(2).key()).endsWith(".webp");
      assertThat(stored.get(0).url()).isEqualTo("https://files.example.com/" + stored.get(0).key());
      assertThat(stored.get(1).contentType()).isEqualTo("image/jpeg");
      assertThat(stored.get(0).size()).isEqualTo(PNG.length);
      assertThat(storage.contentTypes.keySet()).containsExactlyElementsOf(stored.stream().map(StoredImage::key).toList());
      assertThat(stored.stream().map(StoredImage::key).distinct()).hasSize(3);
   }

   @Test
   void oneBadImageStoresNothingAndSaysWhichOne() {
      assertThatThrownBy(() -> uploader.upload("user-1", List.of(PNG, "not an image".getBytes(StandardCharsets.UTF_8))))
         .isInstanceOf(ValidationException.class)
         .hasMessageContaining("Image 2");
      assertThat(storage.contentTypes).isEmpty();
   }

   @Test
   void emptyAndOversizedImagesAreRejected() {
      assertThatThrownBy(() -> uploader.upload("user-1", List.of(new byte[0]))).hasMessageContaining("empty");
      byte[] big = new byte[1025];
      System.arraycopy(PNG, 0, big, 0, PNG.length);
      assertThatThrownBy(() -> uploader.upload("user-1", List.of(big))).hasMessageContaining("larger than 1 KB");
      assertThat(uploader.upload("user-1", List.of(java.util.Arrays.copyOf(PNG, 1024)))).hasSize(1);
   }

   @Test
   void aFailureWhileStoringRemovesTheImagesAlreadyStored() {
      storage.failOnPut = 2;

      assertThatThrownBy(() -> uploader.upload("user-1", List.of(PNG, JPEG, GIF)))
         .isInstanceOf(IllegalStateException.class).hasMessage("storage down");

      assertThat(storage.contentTypes).isEmpty();
      assertThat(storage.deleted).hasSize(2);
   }

   @Test
   void knowsWhoseKeyItIs() {
      assertThat(ImageUploader.isOwnedBy("user-1", "images/user-1/abc.png")).isTrue();
      assertThat(ImageUploader.isOwnedBy("user-1", "images/user-12/abc.png")).isFalse();
      assertThat(ImageUploader.isOwnedBy("user-1", "images/user-2/abc.png")).isFalse();
      assertThat(ImageUploader.isOwnedBy("user-1", "images/user-1/../user-2/abc.png")).isFalse();
      assertThat(ImageUploader.isOwnedBy("user-1", null)).isFalse();
      assertThat(ImageUploader.isOwnedBy(null, "images/user-1/abc.png")).isFalse();
   }
}
