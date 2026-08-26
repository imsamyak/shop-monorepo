package com.ecom.identity.adapter.out.persistence;

import com.ecom.identity.domain.PasswordResetToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens", indexes = @Index(name = "idx_password_reset_tokens_user", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PasswordResetTokenEntity {

   @Id
   private UUID id;

   @Column(name = "user_id", nullable = false)
   private UUID userId;

   @Column(name = "token_hash", nullable = false, unique = true, length = 64)
   private String tokenHash;

   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;

   @Column(name = "created_at", nullable = false, updatable = false)
   private Instant createdAt;

   static PasswordResetTokenEntity from(PasswordResetToken token) {
      PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
      entity.id = token.id();
      entity.userId = token.userId();
      entity.tokenHash = token.tokenHash();
      entity.expiresAt = token.expiresAt();
      entity.createdAt = token.createdAt();
      return entity;
   }

   PasswordResetToken toDomain() {
      return new PasswordResetToken(id, userId, tokenHash, expiresAt, createdAt);
   }
}
