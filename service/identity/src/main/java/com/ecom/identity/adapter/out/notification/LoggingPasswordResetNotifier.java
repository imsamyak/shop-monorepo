package com.ecom.identity.adapter.out.notification;

import com.ecom.identity.config.IdentityProperties;
import com.ecom.identity.port.out.PasswordResetNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Stands in until a mail sender is wired: it sends nothing. With {@code identity.password-reset.log-links=true} (local
 * use only) it logs the link so a reset can be tried by hand; otherwise it only notes that a reset was requested,
 * because a reset link is as good as the password and must not end up in logs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class LoggingPasswordResetNotifier implements PasswordResetNotifier {

   private final IdentityProperties properties;

   @Override
   public void send(String email, String resetLink, Instant expiresAt) {
      if (properties.passwordReset().logLinks()) {
         log.warn("Password reset link for {} (valid until {}): {}", email, expiresAt, resetLink);
      } else {
         log.warn("A password reset was requested for {} but no mail sender is configured; nothing was sent", email);
      }
   }
}
