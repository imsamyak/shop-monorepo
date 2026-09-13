package com.ecom.security.jwt;

import com.ecom.security.UserPrincipal;
import com.ecom.security.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTest {

   private static final String ISSUER = "ecom-identity";

   private final KeyPair keys = RsaKeys.generate();
   private final Clock now = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
   private final JwtSigner signer = new JwtSigner(keys.getPrivate(), RsaKeys.keyId(keys.getPublic()), ISSUER, now);
   private final RsaJwtVerifier verifier = new RsaJwtVerifier(kid -> keys.getPublic(), ISSUER, now);

   private static UserPrincipal seller() {
      return UserPrincipal.of("u-1", "s@example.com", UserRole.ROLE_SELLER, "u-1");
   }

   @Test
   void aSignedTokenVerifiesToTheSamePrincipal() {
      String token = signer.sign(seller(), Duration.ofMinutes(10));

      UserPrincipal principal = verifier.verify(token).orElseThrow();

      assertThat(principal.userId()).isEqualTo("u-1");
      assertThat(principal.email()).isEqualTo("s@example.com");
      assertThat(principal.roles()).containsExactly(UserRole.ROLE_SELLER);
      assertThat(principal.sellerId()).isEqualTo("u-1");
   }

   @Test
   void anExpiredTokenIsRejected() {
      assertThat(verifier.verify(signer.sign(seller(), Duration.ofHours(-1)))).isEmpty();
   }

   @Test
   void aTokenSignedWithAnotherKeyIsRejected() {
      JwtSigner stranger = JwtSigner.forKey(RsaKeys.generate().getPrivate(), ISSUER);

      assertThat(verifier.verify(stranger.sign(seller(), Duration.ofMinutes(10)))).isEmpty();
   }

   @Test
   void aTokenFromAnotherIssuerIsRejected() {
      JwtSigner other = new JwtSigner(keys.getPrivate(), RsaKeys.keyId(keys.getPublic()), "someone-else", now);

      assertThat(verifier.verify(other.sign(seller(), Duration.ofMinutes(10)))).isEmpty();
   }

   @Test
   void anUnsignedAlgNoneTokenIsRejected() {
      Base64.Encoder url = Base64.getUrlEncoder().withoutPadding();
      String header = url.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
      String claims = url.encodeToString(("{\"iss\":\"" + ISSUER + "\",\"sub\":\"u-1\",\"roles\":[\"ROLE_ADMIN\"],\"exp\":"
          + (now.millis() / 1000 + 600) + "}").getBytes());

      assertThat(verifier.verify(header + "." + claims + ".")).isEmpty();
   }

   @Test
   void garbageAndBlankInputAreRejected() {
      assertThat(verifier.verify("garbage")).isEmpty();
      assertThat(verifier.verify("a.b.c")).isEmpty();
      assertThat(verifier.verify("")).isEmpty();
      assertThat(verifier.verify(null)).isEmpty();
   }

   @Test
   void keysSurviveAPemRoundTripAndThePublicKeyIsDerivedFromThePrivateOne() {
      var privateKey = RsaKeys.parsePrivateKey(RsaKeys.toPem(keys.getPrivate()));
      var publicKey = RsaKeys.parsePublicKey(RsaKeys.toPem(RsaKeys.publicFromPrivate(privateKey)));

      assertThat(RsaKeys.keyId(publicKey)).isEqualTo(RsaKeys.keyId(keys.getPublic()));
   }

   @Test
   void theJwkRoundTripsToTheSamePublicKey() {
      var jwk = RsaKeys.toJwk(keys.getPublic());

      var restored = RsaKeys.fromJwk((String) jwk.get("n"), (String) jwk.get("e"));

      assertThat(restored).isEqualTo(keys.getPublic());
      assertThat(jwk).doesNotContainKey("d");
   }

   @SuppressWarnings("unchecked")
   private static ObjectProvider<TokenVerifier> providerOf(TokenVerifier verifier) {
      return new ObjectProvider<>() {
         @Override
         public TokenVerifier getObject() { return verifier; }
         @Override
         public TokenVerifier getIfAvailable() { return verifier; }
         @Override
         public TokenVerifier getIfUnique() { return verifier; }
      };
   }

   @Test
   void theBearerResolverReadsOnlyTheAuthorizationHeader() {
      BearerPrincipalResolver resolver = new BearerPrincipalResolver(providerOf(verifier));
      String token = signer.sign(seller(), Duration.ofMinutes(10));

      MockHttpServletRequest bearer = new MockHttpServletRequest();
      bearer.addHeader("Authorization", "Bearer " + token);
      assertThat(resolver.resolve(bearer)).map(UserPrincipal::userId).contains("u-1");

      MockHttpServletRequest lowerCase = new MockHttpServletRequest();
      lowerCase.addHeader("Authorization", "bearer " + token);
      assertThat(resolver.resolve(lowerCase)).isPresent();

      MockHttpServletRequest basic = new MockHttpServletRequest();
      basic.addHeader("Authorization", "Basic " + token);
      assertThat(resolver.resolve(basic)).isEmpty();

      MockHttpServletRequest forwardedIdentity = new MockHttpServletRequest();
      forwardedIdentity.addHeader("X-User-Id", "u-1");
      forwardedIdentity.addHeader("X-User-Roles", "ROLE_ADMIN");
      assertThat(resolver.resolve(forwardedIdentity)).isEmpty();

      assertThat(resolver.resolve(new MockHttpServletRequest())).isEmpty();
   }

   @Test
   void withoutAVerifierEveryRequestIsAnonymous() {
      BearerPrincipalResolver resolver = new BearerPrincipalResolver(providerOf(null));
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Authorization", "Bearer " + signer.sign(seller(), Duration.ofMinutes(10)));

      assertThat(resolver.resolve(request)).isEqualTo(Optional.empty());
   }
}
