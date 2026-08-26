package com.ecom.identity.service.support;

import com.ecom.shared.exception.validation.InvalidCommandException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/** What an email address and a password must look like. */
public final class CredentialRules {

   private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
   private static final int MIN_PASSWORD_LENGTH = 8;
   private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores everything after 72 bytes

   private CredentialRules() {
   }

   /** Trimmed and lower-cased, so the same address always maps to the same account. */
   public static String normalizeEmail(String email) {
      String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
      if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) {
         throw InvalidCommandException.of("A valid email address is required");
      }
      return normalized;
   }

   public static void validatePassword(String password) {
      if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
         throw InvalidCommandException.of("The password must be at least " + MIN_PASSWORD_LENGTH + " characters");
      }
      if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
         throw InvalidCommandException.of("The password must be at most " + MAX_PASSWORD_BYTES + " bytes");
      }
   }
}
