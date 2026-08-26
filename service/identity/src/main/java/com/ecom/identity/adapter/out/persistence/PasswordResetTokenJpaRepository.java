package com.ecom.identity.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

   Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

   void deleteByUserId(UUID userId);

   void deleteByExpiresAtBefore(Instant instant);
}
