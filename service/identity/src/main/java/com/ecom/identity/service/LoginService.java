package com.ecom.identity.service;

import com.ecom.identity.config.IdentityProperties;
import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.LoginUseCase;
import com.ecom.identity.port.in.usecase.dto.command.LoginCmd;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.identity.port.out.PasswordHasher;
import com.ecom.identity.port.out.UserStore;
import com.ecom.identity.service.support.CredentialRules;
import com.ecom.identity.service.support.SessionIssuer;
import com.ecom.shared.exception.security.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Signs a user in. An unknown email and a wrong password fail the same way and take the same time, and an account is
 * locked for a while after too many failures.
 */
@Service
@Validated
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

   private static final String INVALID_CREDENTIALS = "Invalid email or password";

   private final UserStore users;
   private final PasswordHasher hasher;
   private final SessionIssuer sessions;
   private final IdentityProperties properties;
   private final Clock clock;

   @Override
   public SessionResult execute(LoginCmd cmd) {
      Instant now = clock.instant();
      Optional<User> found = users.findByEmail(CredentialRules.normalizeEmail(cmd.email()));
      if (found.isEmpty() || !found.get().enabled()) {
         hasher.matchesDummy(cmd.password() == null ? "" : cmd.password());
         throw UnauthorizedException.of(INVALID_CREDENTIALS);
      }

      User user = found.get();
      if (user.isLocked(now)) {
         throw UnauthorizedException.of("Too many failed attempts. Try again later.");
      }
      if (!hasher.matches(cmd.password(), user.passwordHash())) {
         user.registerFailedLogin(now, properties.lockout().maxAttempts(), Duration.ofSeconds(properties.lockout().lockSeconds()));
         users.save(user);
         throw UnauthorizedException.of(INVALID_CREDENTIALS);
      }
      if (user.needsLoginStateReset()) {
         user.registerSuccessfulLogin();
         users.save(user);
      }
      return sessions.start(user);
   }
}
