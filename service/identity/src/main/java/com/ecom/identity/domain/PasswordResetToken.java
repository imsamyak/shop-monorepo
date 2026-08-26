package com.ecom.identity.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A pending password reset. Only a hash of the secret is kept, so a leaked table cannot be used to reset anyone's
 * password. A token is single use: using it deletes every pending token of that user.
 */
public record PasswordResetToken(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {

   public static PasswordResetToken issue(UUID userId, String tokenHash, Instant now, Instant expiresAt) {
      return new PasswordResetToken(UUID.randomUUID(), userId, tokenHash, expiresAt, now);
   }

   public boolean isUsableAt(Instant now) {
      return expiresAt.isAfter(now);
   }
}
