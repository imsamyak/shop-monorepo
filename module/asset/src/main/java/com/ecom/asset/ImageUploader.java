package com.ecom.asset;

import com.ecom.shared.exception.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checks and stores images. Every image is checked before any is stored, so a batch with one bad file stores nothing,
 * and if storing fails half way the images already stored are removed again. The result lists the images in the order
 * they were given.
 * <p>
 * Keys are {@code images/<owner>/<random>.<extension>}: random so nobody can guess or overwrite another file, and
 * prefixed with the owner so it is always clear whose file it is (see {@link #isOwnedBy}).
 */
public class ImageUploader {

   private static final String PREFIX = "images/";

   private final AssetStorage storage;
   private final long maxImageBytes;

   public ImageUploader(AssetStorage storage, AssetProperties properties) {
      this.storage = storage;
      this.maxImageBytes = properties.maxImageBytes();
   }

   /**
    * @throws ValidationException if any image is empty, too large or not a supported image
    */
   public List<StoredImage> upload(String ownerId, List<byte[]> images) {
      List<ImageType> types = new ArrayList<>(images.size());
      for (int i = 0; i < images.size(); i++) {
         types.add(check(images.get(i), i + 1));
      }

      List<StoredImage> stored = new ArrayList<>(images.size());
      try {
         for (int i = 0; i < images.size(); i++) {
            ImageType type = types.get(i);
            String key = PREFIX + ownerId + "/" + UUID.randomUUID() + "." + type.extension();
            storage.put(key, images.get(i), type.contentType());
            stored.add(new StoredImage(key, storage.publicUrl(key), type.contentType(), images.get(i).length));
         }
      } catch (RuntimeException failure) {
         stored.forEach(image -> removeQuietly(image.key()));
         throw failure;
      }
      return stored;
   }

   public void delete(String key) {
      storage.delete(key);
   }

   /**
    * Whether the key is one of this owner's images.
    */
   public static boolean isOwnedBy(String ownerId, String key) {
      return ownerId != null && key != null && !key.contains("..") && key.startsWith(PREFIX + ownerId + "/");
   }

   private ImageType check(byte[] content, int position) {
      if (content == null || content.length == 0) {
         throw new ValidationException("Image " + position + " is empty");
      }
      if (content.length > maxImageBytes) {
         throw new ValidationException("Image " + position + " is larger than " + maxImageBytes / 1024 + " KB");
      }
      return ImageType.detect(content).orElseThrow(() ->
         new ValidationException("Image " + position + " is not a JPEG, PNG, GIF or WebP image"));
   }

   private void removeQuietly(String key) {
      try {
         storage.delete(key);
      } catch (RuntimeException ignored) {
         // the original failure is the one to report
      }
   }
}
