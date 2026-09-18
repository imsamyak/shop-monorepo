package com.ecom.security.jwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the identity service's public keys from its JWKS endpoint, lazily and cached. A token signed with a key id that is
 * not cached triggers a refresh (so key rotation needs no restart), at most once per {@code minRefresh} so unknown key
 * ids cannot be used to hammer the identity service. If the identity service is down, the keys already cached keep working.
 */
@Slf4j
public class JwksPublicKeyResolver implements RsaJwtVerifier.PublicKeyResolver {

   private static final ObjectMapper JSON = new ObjectMapper();

   private final URI jwksUri;
   private final HttpClient http;
   private final Duration maxAge;
   private final Duration minRefresh;
   private final Clock clock;

   private Map<String, PublicKey> keys = Map.of();
   private long fetchedAtMillis = 0;
   private long attemptedAtMillis = 0;

   public JwksPublicKeyResolver(String jwksUri, Duration maxAge) {
      this(jwksUri, maxAge, Duration.ofSeconds(5), Clock.systemUTC());
   }

   public JwksPublicKeyResolver(String jwksUri, Duration maxAge, Duration minRefresh, Clock clock) {
      this.jwksUri = URI.create(jwksUri);
      this.maxAge = maxAge;
      this.minRefresh = minRefresh;
      this.clock = clock;
      this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
   }

   @Override
   public synchronized PublicKey resolve(String keyId) {
      long now = clock.millis();
      boolean stale = now - fetchedAtMillis > maxAge.toMillis();
      boolean unknown = keyId == null ? keys.isEmpty() : !keys.containsKey(keyId);
      if ((stale || unknown) && now - attemptedAtMillis >= minRefresh.toMillis()) {
         refresh(now);
      }
      if (keyId != null) {
         return keys.get(keyId);
      }
      return keys.size() == 1 ? keys.values().iterator().next() : null;
   }

   private void refresh(long now) {
      attemptedAtMillis = now;
      try {
         HttpResponse<String> response = http.send(
               HttpRequest.newBuilder(jwksUri).timeout(Duration.ofSeconds(5)).GET().build(),
               HttpResponse.BodyHandlers.ofString());
         if (response.statusCode() != 200) {
            log.warn("JWKS endpoint {} answered HTTP {}", jwksUri, response.statusCode());
            return;
         }
         Map<String, PublicKey> loaded = new HashMap<>();
         for (JsonNode jwk : JSON.readTree(response.body()).path("keys")) {
            if ("RSA".equals(jwk.path("kty").asText()) && !jwk.path("kid").asText().isBlank()) {
               loaded.put(jwk.path("kid").asText(), RsaKeys.fromJwk(jwk.path("n").asText(), jwk.path("e").asText()));
            }
         }
         if (!loaded.isEmpty()) {
            keys = Map.copyOf(loaded);
            fetchedAtMillis = now;
         }
      } catch (Exception e) {
         log.warn("Could not load the signing keys from {}: {}", jwksUri, e.getMessage());
      }
   }
}
