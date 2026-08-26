package com.ecom.identity.port.out;

import com.ecom.identity.domain.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetStore {

   PasswordResetToken save(PasswordResetToken token);

   Optional<PasswordResetToken> findByTokenHash(String tokenHash);

   /**
    * Removes every pending token of a user: before issuing a new one, and once one has been used.
    */
   void deleteByUserId(UUID userId);

   /**
    * Housekeeping: removes tokens whose time has run out.
    */
   void deleteExpired(Instant now);
}
