package com.ecom.identity.service;

import com.ecom.identity.config.IdentityProperties;
import com.ecom.identity.domain.PasswordResetToken;
import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.ForgotPasswordUseCase;
import com.ecom.identity.port.in.usecase.dto.command.ForgotPasswordCmd;
import com.ecom.identity.port.in.usecase.dto.result.Done;
import com.ecom.identity.port.out.PasswordResetNotifier;
import com.ecom.identity.port.out.PasswordResetStore;
import com.ecom.identity.port.out.UserStore;
import com.ecom.identity.service.support.CredentialRules;
import com.ecom.identity.service.support.ResetTokens;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;

/**
 * Starts a password reset. An unknown address gets exactly the same answer as a known one, and a failure to send is
 * logged instead of reported, so the response never tells anyone which addresses have an account.
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ForgotPasswordService implements ForgotPasswordUseCase {

   private final UserStore users;
   private final PasswordResetStore resets;
   private final PasswordResetNotifier notifier;
   private final IdentityProperties properties;
   private final Clock clock;

   @Override
   @Transactional
   public Done execute(ForgotPasswordCmd cmd) {
      Instant now = clock.instant();
      String email = CredentialRules.normalizeEmail(cmd.email());

      resets.deleteExpired(now);
      users.findByEmail(email).filter(User::enabled).ifPresent(user -> issue(user, now));
      return Done.INSTANCE;
   }

   private void issue(User user, Instant now) {
      resets.deleteByUserId(user.id());

      String secret = ResetTokens.newToken();
      Instant expiresAt = now.plusSeconds(properties.passwordReset().ttlSeconds());
      resets.save(PasswordResetToken.issue(user.id(), ResetTokens.hash(secret), now, expiresAt));

      try {
         notifier.send(user.email(), properties.passwordReset().linkBaseUrl() + "?token=" + secret, expiresAt);
      } catch (RuntimeException e) {
         log.error("Could not send the password reset link for user {}", user.id(), e);
      }
   }
}
