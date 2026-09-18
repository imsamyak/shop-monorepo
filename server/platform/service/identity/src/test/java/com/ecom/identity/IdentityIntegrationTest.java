package com.ecom.identity;

import com.ecom.identity.port.in.usecase.CreateUserUseCase;
import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.identity.port.out.PasswordResetNotifier;
import com.ecom.security.UserPrincipal;
import com.ecom.security.UserRole;
import com.ecom.security.jwt.JwtSigner;
import com.ecom.security.jwt.RsaJwtVerifier;
import com.ecom.security.jwt.RsaKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole identity service over HTTP, with H2 for users.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(IdentityIntegrationTest.Mailbox.class)
class IdentityIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final String PASSWORD = "correct horse battery";

   /**
    * Replaces the real notifier so a test can read the link the way the user would read it in their mail.
    */
   @TestConfiguration
   static class Mailbox {
      record Mail(String email, String link) {
         String token() {
            return link.substring(link.indexOf("?token=") + "?token=".length());
         }
      }

      static final List<Mail> SENT = new CopyOnWriteArrayList<>();

      @Bean
      @Primary
      PasswordResetNotifier mailbox() {
         return (email, link, expiresAt) -> SENT.add(new Mail(email, link));
      }

      static Mail lastTo(String email) {
         return SENT.stream().filter(mail -> mail.email().equals(email)).reduce((first, second) -> second).orElseThrow();
      }
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   CreateUserUseCase createUser;

   @Autowired
   JwtSigner signer;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body, HttpResponse<String> raw) {
      String text(String field) {
         return body.path(field).asText();
      }
   }

   private Reply call(String method, String path, String bearer, String body) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
         .header("Content-Type", "application/json");
      if (bearer != null) {
         request.header("Authorization", "Bearer " + bearer);
      }
      request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json, response);
   }

   private static String credentials(String email, String password) {
      return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
   }

   private static String uniqueEmail() {
      return "user-" + UUID.randomUUID() + "@example.com";
   }

   private Reply register(String email) throws Exception {
      Reply reply = call("POST", "/auth/register", null, credentials(email, PASSWORD));
      assertThat(reply.status()).isEqualTo(201);
      return reply;
   }

   private String adminToken() {
      String email = uniqueEmail();
      createUser.execute(new CreateUserCmd(email, PASSWORD, UserRole.ROLE_ADMIN));
      return signer.sign(UserPrincipal.of(UUID.randomUUID().toString(), email, UserRole.ROLE_ADMIN, null), Duration.ofMinutes(5));
   }

   // --- sign up and sign in ---------------------------------------------------------------------------------------

   @Test
   void registerLoginAndMe() throws Exception {
      String email = uniqueEmail();
      Reply registered = register(email);
      assertThat(registered.text("tokenType")).isEqualTo("Bearer");
      assertThat(registered.body().path("user").path("role").asText()).isEqualTo("ROLE_BUYER");

      Reply login = call("POST", "/auth/login", null, credentials(email, PASSWORD));
      assertThat(login.status()).isEqualTo(200);
      assertThat(login.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");

      Reply me = call("GET", "/auth/me", login.text("accessToken"), null);
      assertThat(me.status()).isEqualTo(200);
      assertThat(me.text("email")).isEqualTo(email);
      assertThat(me.text("id")).isEqualTo(login.body().path("user").path("id").asText());
      assertThat(me.body().has("passwordHash")).isFalse();
   }

   @Test
   void meRequiresAValidAccessToken() throws Exception {
      assertThat(call("GET", "/auth/me", null, null).status()).isEqualTo(401);
      assertThat(call("GET", "/auth/me", "garbage", null).status()).isEqualTo(401);
   }

   @Test
   void wrongPasswordAndDuplicateEmailAreRejected() throws Exception {
      String email = uniqueEmail();
      register(email);

      assertThat(call("POST", "/auth/login", null, credentials(email, "wrong password!")).status()).isEqualTo(401);
      assertThat(call("POST", "/auth/register", null, credentials(email, PASSWORD)).status()).isEqualTo(409);
      assertThat(call("POST", "/auth/register", null, credentials(uniqueEmail(), "short")).status()).isEqualTo(400);
      assertThat(call("POST", "/auth/register", null, "{}").status()).isEqualTo(400);
   }

   @Test
   void registrationNeverGrantsAPrivilegedRole() throws Exception {
      Reply reply = call("POST", "/auth/register", null,
         "{\"email\":\"" + uniqueEmail() + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ROLE_ADMIN\"}");

      assertThat(reply.status()).isEqualTo(201);
      assertThat(reply.body().path("user").path("role").asText()).isEqualTo("ROLE_BUYER");
   }

   @Test
   void anyoneCanSignUpAsASellerAndGetsASellerIdInTheirToken() throws Exception {
      String email = uniqueEmail();

      Reply reply = call("POST", "/auth/register/seller", null, credentials(email, PASSWORD));

      assertThat(reply.status()).isEqualTo(201);
      assertThat(reply.body().path("user").path("role").asText()).isEqualTo("ROLE_SELLER");
      String sellerId = reply.body().path("user").path("sellerId").asText();
      assertThat(sellerId).isNotBlank();
      Reply me = call("GET", "/auth/me", reply.body().path("accessToken").asText(), null);
      assertThat(me.status()).isEqualTo(200);
      assertThat(me.body().path("sellerId").asText()).isEqualTo(sellerId);
      assertThat(call("POST", "/auth/login", null, credentials(email, PASSWORD)).status()).isEqualTo(200);
      assertThat(call("POST", "/auth/register/seller", null, credentials(email, PASSWORD)).status()).as("taken").isEqualTo(409);
      assertThat(call("POST", "/auth/register/seller", null, credentials(uniqueEmail(), "short")).status()).isEqualTo(400);
   }

   @Test
   void accountLocksAfterRepeatedFailures() throws Exception {
      String email = uniqueEmail();
      register(email);

      for (int i = 0; i < 3; i++) {
         assertThat(call("POST", "/auth/login", null, credentials(email, "wrong password!")).status()).isEqualTo(401);
      }

      Reply locked = call("POST", "/auth/login", null, credentials(email, PASSWORD));
      assertThat(locked.status()).isEqualTo(401);
      assertThat(locked.text("detail")).contains("Too many");
   }

   @Test
   void noAccountExistsUntilSomeoneCreatesIt() throws Exception {
      assertThat(call("POST", "/auth/login", null, credentials("admin@ecom.dev", "demo-password-123")).status()).isEqualTo(401);
      assertThat(call("POST", "/auth/login", null, credentials("admin@shop.dev", "demo-password-123")).status()).isEqualTo(401);
   }

   @Test
   void onlyAdminsCreateSellers() throws Exception {
      Reply buyer = register(uniqueEmail());
      String sellerEmail = uniqueEmail();
      String body = credentials(sellerEmail, PASSWORD);

      assertThat(call("POST", "/auth/admin/sellers", null, body).status()).isEqualTo(401);
      assertThat(call("POST", "/auth/admin/sellers", buyer.text("accessToken"), body).status()).isEqualTo(403);

      Reply created = call("POST", "/auth/admin/sellers", adminToken(), body);
      assertThat(created.status()).isEqualTo(201);
      assertThat(created.text("role")).isEqualTo("ROLE_SELLER");
      assertThat(created.text("sellerId")).isEqualTo(created.text("id"));

      Reply sellerLogin = call("POST", "/auth/login", null, body);
      assertThat(sellerLogin.status()).isEqualTo(200);
      assertThat(sellerLogin.body().path("user").path("sellerId").asText()).isEqualTo(created.text("id"));
   }

   @Test
   void theJwksVerifiesTokensTheServiceIssued() throws Exception {
      Reply session = register(uniqueEmail());

      Reply jwks = call("GET", "/.well-known/jwks.json", null, null);
      assertThat(jwks.status()).isEqualTo(200);
      JsonNode key = jwks.body().path("keys").get(0);
      assertThat(key.path("kty").asText()).isEqualTo("RSA");
      assertThat(key.has("d")).as("no private material is published").isFalse();
      PublicKey publicKey = RsaKeys.fromJwk(key.path("n").asText(), key.path("e").asText());

      var principal = new RsaJwtVerifier(kid -> kid.equals(key.path("kid").asText()) ? publicKey : null, "ecom-identity")
         .verify(session.text("accessToken"));
      assertThat(principal).isPresent();
      assertThat(principal.get().userId()).isEqualTo(session.body().path("user").path("id").asText());
   }

   @Test
   void serviceTokensComeFromClientCredentials() throws Exception {
      Reply ok = call("POST", "/auth/token", null, "{\"clientId\":\"checkout-service\",\"clientSecret\":\"test-client-secret\"}");
      assertThat(ok.status()).isEqualTo(200);
      assertThat(ok.body().path("expiresIn").asLong()).isEqualTo(300);

      Reply jwks = call("GET", "/.well-known/jwks.json", null, null);
      JsonNode key = jwks.body().path("keys").get(0);
      PublicKey publicKey = RsaKeys.fromJwk(key.path("n").asText(), key.path("e").asText());
      var principal = new RsaJwtVerifier(kid -> publicKey, "ecom-identity").verify(ok.text("accessToken")).orElseThrow();
      assertThat(principal.hasRole(UserRole.ROLE_SYSTEM)).isTrue();

      assertThat(call("POST", "/auth/token", null, "{\"clientId\":\"checkout-service\",\"clientSecret\":\"nope\"}").status()).isEqualTo(401);
   }

   @Test
   void tokensSignedElsewhereOrExpiredAreRejected() throws Exception {
      UserPrincipal admin = UserPrincipal.of(UUID.randomUUID().toString(), null, UserRole.ROLE_ADMIN, null);
      String forged = JwtSigner.forKey(RsaKeys.generate().getPrivate(), "ecom-identity").sign(admin, Duration.ofMinutes(5));
      String expired = signer.sign(admin, Duration.ofHours(-1));
      String body = credentials(uniqueEmail(), PASSWORD);

      assertThat(call("POST", "/auth/admin/sellers", forged, body).status()).isEqualTo(401);
      assertThat(call("POST", "/auth/admin/sellers", expired, body).status()).isEqualTo(401);
   }

   // --- logout ----------------------------------------------------------------------------------------------------

   @Test
   void logoutAnswers204WithOrWithoutAToken() throws Exception {
      Reply session = register(uniqueEmail());

      Reply withToken = call("POST", "/auth/logout", session.text("accessToken"), null);
      assertThat(withToken.status()).isEqualTo(204);
      assertThat(withToken.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");

      assertThat(call("POST", "/auth/logout", null, null).status()).isEqualTo(204);
      assertThat(call("POST", "/auth/logout", "an-expired-or-garbled-token", null).status()).isEqualTo(204);
   }

   // --- forgot and reset password ---------------------------------------------------------------------------------

   @Test
   void forgotThenResetLetsTheUserSignInWithTheNewPassword() throws Exception {
      String email = uniqueEmail();
      register(email);

      Reply forgot = call("POST", "/auth/forgot-password", null, "{\"email\":\"" + email + "\"}");
      assertThat(forgot.status()).isEqualTo(202);
      assertThat(forgot.raw().headers().firstValue("Cache-Control").orElse("")).contains("no-store");
      Mailbox.Mail mail = Mailbox.lastTo(email);
      assertThat(mail.link()).startsWith("http://storefront.test/reset-password?token=");

      Reply reset = call("POST", "/auth/reset-password", null,
         "{\"token\":\"" + mail.token() + "\",\"newPassword\":\"a brand new password\"}");
      assertThat(reset.status()).isEqualTo(204);

      assertThat(call("POST", "/auth/login", null, credentials(email, "a brand new password")).status()).isEqualTo(200);
      assertThat(call("POST", "/auth/login", null, credentials(email, PASSWORD)).status()).isEqualTo(401);
   }

   @Test
   void forgotAnswersTheSameForAnUnknownAddress() throws Exception {
      int before = Mailbox.SENT.size();

      Reply forgot = call("POST", "/auth/forgot-password", null, "{\"email\":\"" + uniqueEmail() + "\"}");

      assertThat(forgot.status()).isEqualTo(202);
      assertThat(forgot.body().isEmpty()).isTrue();
      assertThat(Mailbox.SENT).hasSize(before);
   }

   @Test
   void aResetLinkWorksOnlyOnceOverHttp() throws Exception {
      String email = uniqueEmail();
      register(email);
      call("POST", "/auth/forgot-password", null, "{\"email\":\"" + email + "\"}");
      String body = "{\"token\":\"" + Mailbox.lastTo(email).token() + "\",\"newPassword\":\"a brand new password\"}";

      assertThat(call("POST", "/auth/reset-password", null, body).status()).isEqualTo(204);

      Reply again = call("POST", "/auth/reset-password", null, body);
      assertThat(again.status()).isEqualTo(400);
      assertThat(again.text("detail")).isEqualTo("The reset link is invalid or has expired");
   }

   @Test
   void resetRejectsAnUnknownTokenAndABadRequest() throws Exception {
      assertThat(call("POST", "/auth/reset-password", null, "{\"token\":\"nope\",\"newPassword\":\"a brand new password\"}").status())
         .isEqualTo(400);
      assertThat(call("POST", "/auth/reset-password", null, "{\"token\":\"\",\"newPassword\":\"\"}").status()).isEqualTo(400);
      assertThat(call("POST", "/auth/forgot-password", null, "{}").status()).isEqualTo(400);
      assertThat(call("POST", "/auth/forgot-password", null, "{\"email\":\"not-an-email\"}").status()).isEqualTo(400);
   }

   @Test
   void aWeakNewPasswordIsRejectedAndTheLinkSurvives() throws Exception {
      String email = uniqueEmail();
      register(email);
      call("POST", "/auth/forgot-password", null, "{\"email\":\"" + email + "\"}");
      String token = Mailbox.lastTo(email).token();

      assertThat(call("POST", "/auth/reset-password", null, "{\"token\":\"" + token + "\",\"newPassword\":\"short\"}").status())
         .isEqualTo(400);
      assertThat(call("POST", "/auth/reset-password", null, "{\"token\":\"" + token + "\",\"newPassword\":\"a long enough one\"}").status())
         .isEqualTo(204);
   }
}
