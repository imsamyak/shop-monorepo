package com.ecom.identity.domain;

import com.ecom.security.UserPrincipal;
import com.ecom.security.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A person who can sign in. A seller's seller id is their user id, so ownership checks elsewhere need no mapping.
 */
public class User {

   private final UUID id;
   private final String email;
   private String passwordHash;
   private final UserRole role;
   private final Instant createdAt;
   private int failedLoginAttempts;
   private Instant lockedUntil;
   private final boolean enabled;

   public User(UUID id, String email, String passwordHash, UserRole role, Instant createdAt,
               int failedLoginAttempts, Instant lockedUntil, boolean enabled) {
      this.id = id;
      this.email = email;
      this.passwordHash = passwordHash;
      this.role = role;
      this.createdAt = createdAt;
      this.failedLoginAttempts = failedLoginAttempts;
      this.lockedUntil = lockedUntil;
      this.enabled = enabled;
   }

   public static User create(String email, String passwordHash, UserRole role, Instant now) {
      return new User(UUID.randomUUID(), email, passwordHash, role, now, 0, null, true);
   }

   public boolean isLocked(Instant now) {
      return lockedUntil != null && lockedUntil.isAfter(now);
   }

   /**
    * Counts a failed sign-in and locks the account once {@code maxAttempts} is reached.
    */
   public void registerFailedLogin(Instant now, int maxAttempts, Duration lockFor) {
      failedLoginAttempts++;
      if (failedLoginAttempts >= maxAttempts) {
         lockedUntil = now.plus(lockFor);
         failedLoginAttempts = 0;
      }
   }

   public void registerSuccessfulLogin() {
      failedLoginAttempts = 0;
      lockedUntil = null;
   }

   public boolean needsLoginStateReset() {
      return failedLoginAttempts != 0 || lockedUntil != null;
   }

   /**
    * Replaces the password. Whoever proved they own the account by reset link also gets any lockout lifted.
    */
   public void changePassword(String newPasswordHash) {
      this.passwordHash = newPasswordHash;
      registerSuccessfulLogin();
   }

   public UserPrincipal toPrincipal() {
      String sellerId = role == UserRole.ROLE_SELLER ? id.toString() : null;
      return UserPrincipal.of(id.toString(), email, role, sellerId);
   }

   public UUID id() {
      return id;
   }

   public String email() {
      return email;
   }

   public String passwordHash() {
      return passwordHash;
   }

   public UserRole role() {
      return role;
   }

   public Instant createdAt() {
      return createdAt;
   }

   public int failedLoginAttempts() {
      return failedLoginAttempts;
   }

   public Instant lockedUntil() {
      return lockedUntil;
   }

   public boolean enabled() {
      return enabled;
   }
}
