package com.ecom.checkout.adapter.out.client.identity;

import com.ecom.checkout.adapter.out.client.ClientSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * The access token this service presents when it calls inventory. It logs in to identity as a service client and keeps
 * the token until shortly before it expires, so identity is not called for every request.
 */
@Component
public class ServiceTokenProvider {

   private static final String DEPENDENCY = "identity-service";
   private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);

   private final RestClient restClient;
   private final String clientId;
   private final String clientSecret;

   private String token;
   private Instant expiresAt = Instant.MIN;

   public ServiceTokenProvider(
         @Value("${services.identity.base-url}") String baseUrl,
         @Value("${services.identity.client-id}") String clientId,
         @Value("${services.identity.client-secret}") String clientSecret,
         @Value("${services.identity.connect-timeout-ms:2000}") int connectTimeoutMs,
         @Value("${services.identity.read-timeout-ms:5000}") int readTimeoutMs) {
      this.restClient = ClientSupport.restClient(baseUrl, connectTimeoutMs, readTimeoutMs);
      this.clientId = clientId;
      this.clientSecret = clientSecret;
   }

   public synchronized String token() {
      if (token == null || Instant.now().isAfter(expiresAt.minus(REFRESH_MARGIN))) {
         login();
      }
      return token;
   }

   private void login() {
      String body = ClientSupport.call(DEPENDENCY,
            () -> restClient.post()
                  .uri("/api/auth/token")
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("clientId", clientId, "clientSecret", clientSecret))
                  .retrieve()
                  .body(String.class),
            (status, reason) -> new IllegalStateException(
                  "Identity refused the service login (HTTP " + status + "): " + reason));
      JsonNode json = ClientSupport.parse(body);
      token = json.path("accessToken").asText();
      expiresAt = Instant.now().plusSeconds(json.path("expiresIn").asLong(0));
   }
}
