package com.ecom.identity;

import com.ecom.identity.config.IdentityProperties;
import com.ecom.identity.domain.PasswordResetToken;
import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.identity.port.in.usecase.dto.command.ForgotPasswordCmd;
import com.ecom.identity.port.in.usecase.dto.command.IssueServiceTokenCmd;
import com.ecom.identity.port.in.usecase.dto.command.LoginCmd;
import com.ecom.identity.port.in.usecase.dto.command.RegisterBuyerCmd;
import com.ecom.identity.port.in.usecase.dto.command.ResetPasswordCmd;
import com.ecom.identity.port.in.usecase.dto.result.ServiceTokenResult;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.identity.port.in.usecase.dto.result.UserResult;
import com.ecom.identity.port.out.PasswordHasher;
import com.ecom.identity.port.out.PasswordResetNotifier;
import com.ecom.identity.port.out.PasswordResetStore;
import com.ecom.identity.port.out.UserStore;
import com.ecom.identity.service.CreateUserService;
import com.ecom.identity.service.ForgotPasswordService;
import com.ecom.identity.service.IssueServiceTokenService;
import com.ecom.identity.service.LoginService;
import com.ecom.identity.service.RegisterBuyerService;
import com.ecom.identity.service.ResetPasswordService;
import com.ecom.identity.service.support.SessionIssuer;
import com.ecom.security.UserRole;
import com.ecom.security.jwt.JwtSigner;
import com.ecom.security.jwt.RsaJwtVerifier;
import com.ecom.security.jwt.RsaKeys;
import com.ecom.shared.exception.conflict.DuplicateResourceException;
import com.ecom.shared.exception.security.UnauthorizedException;
import com.ecom.shared.exception.validation.InvalidCommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityServiceTest {

   private static final String BASE_URL = "http://storefront.test/reset-password";

   /**
    * A clock the test can move.
    */
   static class MutableClock extends Clock {
      Instant now = Instant.parse("2030-01-01T00:00:00Z");

      void advanceSeconds(long seconds) {
         now = now.plusSeconds(seconds);
      }

      @Override
      public java.time.ZoneId getZone() {
         return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(java.time.ZoneId zone) {
         return this;
      }

      @Override
      public Instant instant() {
         return now;
      }
   }

   static class InMemoryUsers implements UserStore {
      final Map<UUID, User> byId = new HashMap<>();

      @Override
      public Optional<User> findByEmail(String email) {
         return byId.values().stream().filter(u -> u.email().equals(email)).findFirst();
      }

      @Override
      public Optional<User> findById(UUID id) {
         return Optional.ofNullable(byId.get(id));
      }

      @Override
      public User save(User user) {
         byId.put(user.id(), user);
         return user;
      }
   }

   static class InMemoryResets implements PasswordResetStore {
      final Map<UUID, PasswordResetToken> byId = new HashMap<>();

      @Override
      public PasswordResetToken save(PasswordResetToken token) {
         byId.put(token.id(), token);
         return token;
      }

      @Override
      public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
         return byId.values().stream().filter(t -> t.tokenHash().equals(tokenHash)).findFirst();
      }

      @Override
      public void deleteByUserId(UUID userId) {
         byId.values().removeIf(t -> t.userId().equals(userId));
      }

      @Override
      public void deleteExpired(Instant now) {
         byId.values().removeIf(t -> !t.expiresAt().isAfter(now));
      }
   }

   /**
    * Keeps what would have been mailed, so a test can read the link the way the user would.
    */
   static class RecordingNotifier implements PasswordResetNotifier {
      record Mail(String email, String link, Instant expiresAt) {
         String token() {
            return link.substring(link.indexOf("?token=") + "?token=".length());
         }
      }

      final List<Mail> sent = new ArrayList<>();
      boolean failing;

      @Override
      public void send(String email, String resetLink, Instant expiresAt) {
         if (failing) {
            throw new IllegalStateException("mail server is down");
         }
         sent.add(new Mail(email, resetLink, expiresAt));
      }

      Mail last() {
         return sent.get(sent.size() - 1);
      }
   }

   /**
    * Not BCrypt: the hashing algorithm is not what is being tested here, and it keeps the test fast.
    */
   static class PlainHasher implements PasswordHasher {
      @Override
      public String hash(String rawPassword) {
         return "hashed:" + rawPassword;
      }

      @Override
      public boolean matches(String rawPassword, String hash) {
         return hash(rawPassword).equals(hash);
      }

      @Override
      public void matchesDummy(String rawPassword) {
      }
   }

   /**
    * The use cases under test, with short names so each scenario reads as one line per step.
    */
   record Flows(RegisterBuyerService registerBuyer, LoginService loginService, CreateUserService createUser,
                IssueServiceTokenService serviceTokens, ForgotPasswordService forgotPassword,
                ResetPasswordService resetPassword) {

      SessionResult register(String email, String password) {
         return registerBuyer.execute(new RegisterBuyerCmd(email, password));
      }

      UserResult createSeller(String email, String password) {
         return createUser.execute(new CreateUserCmd(email, password, UserRole.ROLE_SELLER));
      }

      SessionResult login(String email, String password) {
         return loginService.execute(new LoginCmd(email, password));
      }

      ServiceTokenResult serviceToken(String clientId, String clientSecret) {
         return serviceTokens.execute(new IssueServiceTokenCmd(clientId, clientSecret));
      }

      void forgot(String email) {
         forgotPassword.execute(new ForgotPasswordCmd(email));
      }

      void reset(String token, String newPassword) {
         resetPassword.execute(new ResetPasswordCmd(token, newPassword));
      }
   }

   private final MutableClock clock = new MutableClock();
   private final KeyPair keys = RsaKeys.generate();
   private final InMemoryUsers users = new InMemoryUsers();
   private final InMemoryResets resets = new InMemoryResets();
   private final RecordingNotifier mail = new RecordingNotifier();
   private Flows identity;
   private RsaJwtVerifier verifier;

   @BeforeEach
   void setUp() {
      IdentityProperties properties = new IdentityProperties(
         new IdentityProperties.Jwt(null, null, "ecom-identity", 600, 300),
         new IdentityProperties.Lockout(3, 900),
         new IdentityProperties.PasswordReset(1800, BASE_URL, false),
         Map.of("checkout-service", "s3cret"));
      JwtSigner signer = new JwtSigner(keys.getPrivate(), RsaKeys.keyId(keys.getPublic()), "ecom-identity", clock);
      PlainHasher hasher = new PlainHasher();
      SessionIssuer sessions = new SessionIssuer(signer, properties);
      CreateUserService createUser = new CreateUserService(users, hasher, clock);
      identity = new Flows(
         new RegisterBuyerService(createUser, users, sessions),
         new LoginService(users, hasher, sessions, properties, clock),
         createUser,
         new IssueServiceTokenService(properties, signer),
         new ForgotPasswordService(users, resets, mail, properties, clock),
         new ResetPasswordService(users, resets, hasher, clock));
      verifier = new RsaJwtVerifier(kid -> keys.getPublic(), "ecom-identity", clock);
   }

   // --- sign up and sign in (ported from the old service) ---------------------------------------------------------

   @Test
   void registerCreatesABuyerAndStartsASession() {
      SessionResult session = identity.register("  Alice@Example.COM ", "correct horse");

      assertThat(session.user().email()).isEqualTo("alice@example.com");
      assertThat(session.user().role()).isEqualTo("ROLE_BUYER");
      var principal = verifier.verify(session.accessToken()).orElseThrow();
      assertThat(principal.userId()).isEqualTo(session.user().id());
      assertThat(principal.roles()).containsExactly(UserRole.ROLE_BUYER);
      assertThat(principal.sellerId()).isNull();
      assertThat(session.accessExpiresIn()).isEqualTo(600);
   }

   @Test
   void registrationRejectsDuplicatesWeakPasswordsAndBadEmails() {
      identity.register("a@example.com", "correct horse");

      assertThatThrownBy(() -> identity.register("A@example.com", "another password")).isInstanceOf(DuplicateResourceException.class);
      assertThatThrownBy(() -> identity.register("b@example.com", "short")).isInstanceOf(InvalidCommandException.class);
      assertThatThrownBy(() -> identity.register("b@example.com", "x".repeat(73))).isInstanceOf(InvalidCommandException.class);
      assertThatThrownBy(() -> identity.register("not-an-email", "correct horse")).isInstanceOf(InvalidCommandException.class);
   }

   @Test
   void sellersGetTheirOwnUserIdAsSellerId() {
      UserResult seller = identity.createSeller("shop@example.com", "correct horse");

      SessionResult session = identity.login("shop@example.com", "correct horse");
      var principal = verifier.verify(session.accessToken()).orElseThrow();
      assertThat(principal.roles()).containsExactly(UserRole.ROLE_SELLER);
      assertThat(principal.sellerId()).isEqualTo(seller.id());
   }

   @Test
   void loginFailsTheSameWayForUnknownEmailAndWrongPassword() {
      identity.register("a@example.com", "correct horse");

      UnauthorizedException unknown = catchUnauthorized(() -> identity.login("nobody@example.com", "whatever123"));
      UnauthorizedException wrong = catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));

      assertThat(unknown.getMessage()).isEqualTo(wrong.getMessage());
   }

   @Test
   void accountLocksAfterRepeatedFailuresAndUnlocksLater() {
      identity.register("a@example.com", "correct horse");
      for (int i = 0; i < 3; i++) {
         catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      }

      assertThat(catchUnauthorized(() -> identity.login("a@example.com", "correct horse")).getMessage()).contains("Too many");

      clock.advanceSeconds(901);
      assertThat(identity.login("a@example.com", "correct horse").accessToken()).isNotBlank();
   }

   @Test
   void aSuccessfulLoginResetsTheFailureCounter() {
      identity.register("a@example.com", "correct horse");
      catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      identity.login("a@example.com", "correct horse");

      catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      assertThat(identity.login("a@example.com", "correct horse")).isNotNull(); // 2 + 2 failures, never 3 in a row
   }

   @Test
   void serviceTokensRequireTheConfiguredSecret() {
      ServiceTokenResult token = identity.serviceToken("checkout-service", "s3cret");

      var principal = verifier.verify(token.accessToken()).orElseThrow();
      assertThat(principal.userId()).isEqualTo("checkout-service");
      assertThat(principal.roles()).containsExactly(UserRole.ROLE_SYSTEM);
      assertThat(token.expiresIn()).isEqualTo(300);

      assertThatThrownBy(() -> identity.serviceToken("checkout-service", "wrong")).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> identity.serviceToken("unknown", "s3cret")).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> identity.serviceToken(null, null)).isInstanceOf(UnauthorizedException.class);
   }

   // --- forgot and reset password ---------------------------------------------------------------------------------

   @Test
   void forgotPasswordMailsALinkAndKeepsOnlyAHashOfTheSecret() {
      identity.register("a@example.com", "correct horse");

      identity.forgot("  A@Example.com ");

      RecordingNotifier.Mail mailed = mail.last();
      assertThat(mail.sent).hasSize(1);
      assertThat(mailed.email()).isEqualTo("a@example.com");
      assertThat(mailed.link()).startsWith(BASE_URL + "?token=");
      assertThat(mailed.expiresAt()).isEqualTo(clock.instant().plusSeconds(1800));
      assertThat(resets.byId.values()).hasSize(1)
         .allSatisfy(stored -> assertThat(stored.tokenHash()).isNotEqualTo(mailed.token()).hasSize(64));
   }

   @Test
   void forgotPasswordAnswersTheSameForAnUnknownAddressAndSendsNothing() {
      identity.forgot("nobody@example.com");

      assertThat(mail.sent).isEmpty();
      assertThat(resets.byId).isEmpty();
   }

   @Test
   void forgotPasswordStillRejectsAMalformedAddress() {
      assertThatThrownBy(() -> identity.forgot("not-an-email")).isInstanceOf(InvalidCommandException.class);
   }

   @Test
   void aFailingMailSenderDoesNotRevealThatTheAddressHasAnAccount() {
      identity.register("a@example.com", "correct horse");
      mail.failing = true;

      identity.forgot("a@example.com"); // does not throw, exactly like an unknown address
   }

   @Test
   void resetPasswordChangesThePasswordAndTheOldOneStopsWorking() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");

      identity.reset(mail.last().token(), "brand new password");

      assertThat(identity.login("a@example.com", "brand new password").accessToken()).isNotBlank();
      catchUnauthorized(() -> identity.login("a@example.com", "correct horse"));
   }

   @Test
   void aResetLinkWorksOnlyOnce() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");
      String token = mail.last().token();
      identity.reset(token, "brand new password");

      assertThatThrownBy(() -> identity.reset(token, "another password"))
         .isInstanceOf(InvalidCommandException.class)
         .hasMessage("The reset link is invalid or has expired");
      assertThat(resets.byId).isEmpty();
   }

   @Test
   void anExpiredLinkIsRejected() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");
      String token = mail.last().token();

      clock.advanceSeconds(1801);

      assertThatThrownBy(() -> identity.reset(token, "brand new password")).isInstanceOf(InvalidCommandException.class);
      catchUnauthorized(() -> identity.login("a@example.com", "brand new password"));
   }

   @Test
   void aNewRequestReplacesTheEarlierLink() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");
      String first = mail.last().token();
      identity.forgot("a@example.com");
      String second = mail.last().token();

      assertThatThrownBy(() -> identity.reset(first, "brand new password")).isInstanceOf(InvalidCommandException.class);
      identity.reset(second, "brand new password");
   }

   @Test
   void anUnknownOrGarbledLinkIsRejectedWithTheSameMessage() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");

      assertThatThrownBy(() -> identity.reset("not-a-real-token", "brand new password"))
         .isInstanceOf(InvalidCommandException.class)
         .hasMessage("The reset link is invalid or has expired");
   }

   @Test
   void aWeakNewPasswordIsRejectedAndTheLinkStaysUsable() {
      identity.register("a@example.com", "correct horse");
      identity.forgot("a@example.com");
      String token = mail.last().token();

      assertThatThrownBy(() -> identity.reset(token, "short")).isInstanceOf(InvalidCommandException.class);

      identity.reset(token, "long enough password");
   }

   @Test
   void resettingThePasswordLiftsALockout() {
      identity.register("a@example.com", "correct horse");
      for (int i = 0; i < 3; i++) {
         catchUnauthorized(() -> identity.login("a@example.com", "wrong password"));
      }
      assertThat(catchUnauthorized(() -> identity.login("a@example.com", "correct horse")).getMessage()).contains("Too many");

      identity.forgot("a@example.com");
      identity.reset(mail.last().token(), "brand new password");

      assertThat(identity.login("a@example.com", "brand new password").accessToken()).isNotBlank();
   }

   @Test
   void expiredLinksOfOtherUsersAreCleanedUpWhenANewRequestComes() {
      identity.register("a@example.com", "correct horse");
      identity.register("b@example.com", "correct horse");
      identity.forgot("a@example.com");
      clock.advanceSeconds(1801);

      identity.forgot("b@example.com");

      assertThat(resets.byId.values()).hasSize(1)
         .allSatisfy(stored -> assertThat(users.byId.get(stored.userId()).email()).isEqualTo("b@example.com"));
   }

   private static UnauthorizedException catchUnauthorized(Runnable action) {
      try {
         action.run();
      } catch (UnauthorizedException e) {
         return e;
      }
      throw new AssertionError("Expected UnauthorizedException");
   }
}
