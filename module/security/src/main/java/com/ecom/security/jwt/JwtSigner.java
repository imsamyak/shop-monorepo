package com.ecom.security.jwt;

import com.ecom.security.UserPrincipal;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Issues RS256 access tokens. Only the identity service holds the private key; every other service verifies with the
 * public key (see {@link RsaJwtVerifier}).
 */
public class JwtSigner {

   private static final ObjectMapper JSON = new ObjectMapper();

   private final PrivateKey privateKey;
   private final String keyId;
   private final String issuer;
   private final Clock clock;

   public JwtSigner(PrivateKey privateKey, String keyId, String issuer) {
      this(privateKey, keyId, issuer, Clock.systemUTC());
   }

   public JwtSigner(PrivateKey privateKey, String keyId, String issuer, Clock clock) {
      this.privateKey = privateKey;
      this.keyId = keyId;
      this.issuer = issuer;
      this.clock = clock;
   }

   public static JwtSigner forKey(PrivateKey privateKey, String issuer) {
      PublicKey publicKey = RsaKeys.publicFromPrivate(privateKey);
      return new JwtSigner(privateKey, RsaKeys.keyId(publicKey), issuer);
   }

   public String sign(UserPrincipal principal, Duration ttl) {
      try {
         long now = clock.millis() / 1000;

         Map<String, Object> header = new LinkedHashMap<>();
         header.put("alg", "RS256");
         header.put("typ", "JWT");
         header.put("kid", keyId);

         Map<String, Object> claims = new LinkedHashMap<>();
         claims.put("iss", issuer);
         claims.put("sub", principal.userId());
         claims.put("email", principal.email());
         claims.put("sellerId", principal.sellerId());
         claims.put("roles", principal.roles().stream().map(Enum::name).sorted().collect(Collectors.toList()));
         claims.put("iat", now);
         claims.put("exp", now + ttl.toSeconds());
         claims.put("jti", UUID.randomUUID().toString());

         String signingInput = encode(header) + "." + encode(claims);
         Signature signature = Signature.getInstance("SHA256withRSA");
         signature.initSign(privateKey);
         signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
         return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
      } catch (GeneralSecurityException | com.fasterxml.jackson.core.JsonProcessingException e) {
         throw new IllegalStateException("Could not sign the token", e);
      }
   }

   private static String encode(Map<String, Object> value) throws com.fasterxml.jackson.core.JsonProcessingException {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(value));
   }
}
