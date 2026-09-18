package com.ecom.asset;

import java.util.Optional;

/**
 * The image formats that can be uploaded. The type is read from the file's own first bytes, never from the name or
 * the content type the client declares, so a script renamed to {@code .png} is not accepted. SVG is left out on
 * purpose: it can carry scripts.
 */
public enum ImageType {
   JPEG("image/jpeg", "jpg"),
   PNG("image/png", "png"),
   GIF("image/gif", "gif"),
   WEBP("image/webp", "webp");

   private final String contentType;
   private final String extension;

   ImageType(String contentType, String extension) {
      this.contentType = contentType;
      this.extension = extension;
   }

   public String contentType() {
      return contentType;
   }

   public String extension() {
      return extension;
   }

   /**
    * @return the type of the image, or empty if the bytes are not a supported image
    */
   public static Optional<ImageType> detect(byte[] content) {
      if (content == null) {
         return Optional.empty();
      }
      if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
         return Optional.of(JPEG);
      }
      if (startsWith(content, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
         return Optional.of(PNG);
      }
      if (startsWith(content, 'G', 'I', 'F', '8') && content.length > 4 && (content[4] == '7' || content[4] == '9')) {
         return Optional.of(GIF);
      }
      if (content.length >= 12 && startsWith(content, 'R', 'I', 'F', 'F') && matches(content, 8, 'W', 'E', 'B', 'P')) {
         return Optional.of(WEBP);
      }
      return Optional.empty();
   }

   private static boolean startsWith(byte[] content, int... expected) {
      return matches(content, 0, expected);
   }

   private static boolean matches(byte[] content, int offset, int... expected) {
      if (content.length < offset + expected.length) {
         return false;
      }
      for (int i = 0; i < expected.length; i++) {
         if ((content[offset + i] & 0xFF) != expected[i]) {
            return false;
         }
      }
      return true;
   }
}
