package com.ecom.identity.adapter.out.persistence;

import com.ecom.identity.domain.PasswordResetToken;
import com.ecom.identity.port.out.PasswordResetStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
class PasswordResetPersistenceAdapter implements PasswordResetStore {

   private final PasswordResetTokenJpaRepository repository;

   PasswordResetPersistenceAdapter(PasswordResetTokenJpaRepository repository) {
      this.repository = repository;
   }

   @Override
   public PasswordResetToken save(PasswordResetToken token) {
      return repository.saveAndFlush(PasswordResetTokenEntity.from(token)).toDomain();
   }

   @Override
   public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
      return repository.findByTokenHash(tokenHash).map(PasswordResetTokenEntity::toDomain);
   }

   @Override
   @Transactional
   public void deleteByUserId(UUID userId) {
      repository.deleteByUserId(userId);
      repository.flush();
   }

   @Override
   @Transactional
   public void deleteExpired(Instant now) {
      repository.deleteByExpiresAtBefore(now);
      repository.flush();
   }
}
