package com.ecom.identity.adapter.out.persistence;

import com.ecom.identity.domain.User;
import com.ecom.security.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserEntity {

   @Id
   private UUID id;

   @Column(nullable = false, unique = true, length = 254)
   private String email;

   @Column(name = "password_hash", nullable = false, length = 100)
   private String passwordHash;

   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private UserRole role;

   @Column(name = "failed_login_attempts", nullable = false)
   private int failedLoginAttempts;

   @Column(name = "locked_until")
   private Instant lockedUntil;

   @Column(nullable = false)
   private boolean enabled;

   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt;

   @Version
   private long version;

   static UserEntity from(User user) {
      UserEntity entity = new UserEntity();
      entity.id = user.id();
      entity.email = user.email();
      entity.passwordHash = user.passwordHash();
      entity.role = user.role();
      entity.createdAt = user.createdAt();
      entity.apply(user);
      return entity;
   }

   void apply(User user) {
      this.passwordHash = user.passwordHash();
      this.failedLoginAttempts = user.failedLoginAttempts();
      this.lockedUntil = user.lockedUntil();
      this.enabled = user.enabled();
   }

   User toDomain() {
      return new User(id, email, passwordHash, role, createdAt, failedLoginAttempts, lockedUntil, enabled);
   }
}
