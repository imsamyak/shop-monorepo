package com.ecom.security.jwt;

import com.ecom.security.UserPrincipal;
import com.ecom.security.UserRole;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Verifies RS256 tokens: the header must say RS256 (so {@code alg: none} and HMAC "algorithm confusion" tokens are
 * rejected), the signature must match the key for the token's {@code kid}, and the token must not be expired and
 * must come from the configured issuer.
 */
@Slf4j
public class RsaJwtVerifier implements TokenVerifier {

   /** Finds the public key for a key id (the id may be null for tokens without one). */
   @FunctionalInterface
   public interface PublicKeyResolver {
      PublicKey resolve(String keyId);
   }

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final long CLOCK_SKEW_SECONDS = 30;

   private final PublicKeyResolver keys;
   private final String issuer;
   private final Clock clock;

   public RsaJwtVerifier(PublicKeyResolver keys, String issuer) {
      this(keys, issuer, Clock.systemUTC());
   }

   public RsaJwtVerifier(PublicKeyResolver keys, String issuer, Clock clock) {
      this.keys = keys;
      this.issuer = issuer;
      this.clock = clock;
   }

   @Override
   public Optional<UserPrincipal> verify(String bearerToken) {
      if (bearerToken == null || bearerToken.isBlank()) {
         return Optional.empty();
      }
      try {
         String token = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7).trim() : bearerToken.trim();
         String[] parts = token.split("\\.");
         if (parts.length != 3) {
            return Optional.empty();
         }

         Map<String, Object> header = read(parts[0]);
         if (!"RS256".equals(header.get("alg"))) {
            return Optional.empty();
         }

         PublicKey key = keys.resolve((String) header.get("kid"));
         if (key == null || !signatureMatches(key, parts[0] + "." + parts[1], parts[2])) {
            log.debug("Rejected JWT: unknown key or bad signature");
            return Optional.empty();
         }

         Map<String, Object> claims = read(parts[1]);
         if (!issuer.equals(claims.get("iss"))) {
            return Optional.empty();
         }
         if (!(claims.get("exp") instanceof Number exp) || exp.longValue() + CLOCK_SKEW_SECONDS < clock.millis() / 1000) {
            return Optional.empty();
         }
         if (claims.get("nbf") instanceof Number nbf && nbf.longValue() - CLOCK_SKEW_SECONDS > clock.millis() / 1000) {
            return Optional.empty();
         }

         String userId = (String) claims.get("sub");
         Set<UserRole> roles = new HashSet<>();
         if (claims.get("roles") instanceof List<?> list) {
            for (Object item : list) {
               UserRole role = UserRole.fromString(String.valueOf(item));
               if (role != null) roles.add(role);
            }
         }
         if (userId == null || userId.isBlank() || roles.isEmpty()) {
            return Optional.empty();
         }
         return Optional.of(new UserPrincipal(userId, (String) claims.get("email"), roles, (String) claims.get("sellerId")));
      } catch (Exception e) {
         log.debug("Rejected JWT: {}", e.getMessage());
         return Optional.empty();
      }
   }

   private static boolean signatureMatches(PublicKey key, String signingInput, String signature) throws GeneralSecurityException {
      Signature verifier = Signature.getInstance("SHA256withRSA");
      verifier.initVerify(key);
      verifier.update(signingInput.getBytes(StandardCharsets.US_ASCII));
      return verifier.verify(Base64.getUrlDecoder().decode(signature));
   }

   private static Map<String, Object> read(String part) throws java.io.IOException {
      return JSON.readValue(Base64.getUrlDecoder().decode(part), new TypeReference<Map<String, Object>>() {});
   }
}
