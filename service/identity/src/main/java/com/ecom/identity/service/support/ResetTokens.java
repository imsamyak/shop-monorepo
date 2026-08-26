package com.ecom.identity.service.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Makes and fingerprints password-reset secrets. The secret is 256 random bits, so a fast hash is enough to store it
 * safely; unlike a password it cannot be guessed.
 */
public final class ResetTokens {

   private static final SecureRandom RANDOM = new SecureRandom();

   private ResetTokens() {
   }

   public static String newToken() {
      byte[] bytes = new byte[32];
      RANDOM.nextBytes(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }

   public static String hash(String token) {
      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
         return HexFormat.of().formatHex(digest);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException(e);
      }
   }
}
