package com.ecom.checkout.adapter.out.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.shared.exception.dependency.DependencyUnavailableException;
import com.ecom.shared.json.JsonMappers;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Shared plumbing for the HTTP clients: every call has a connect and a read timeout (a hung dependency must never
 * hold a request thread forever), JSON is parsed leniently, and failures are classified.
 */
public final class ClientSupport {

   public static final ObjectMapper JSON = JsonMappers.messaging();

   private ClientSupport() {
   }

   public static RestClient restClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
      HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .build();
      JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
      requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
      return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
   }

   public static JsonNode parse(String body) {
      try {
         return JSON.readTree(body == null || body.isBlank() ? "{}" : body);
      } catch (Exception e) {
         throw new IllegalStateException("Unreadable response body", e);
      }
   }

   /** The human-readable reason in an RFC 9457 problem response, falling back to the raw body. */
   public static String reason(RestClientResponseException ex) {
      String body = ex.getResponseBodyAsString();
      try {
         JsonNode detail = JSON.readTree(body).path("detail");
         if (!detail.isMissingNode() && !detail.asText().isBlank()) {
            return detail.asText();
         }
      } catch (Exception ignored) {
         // not JSON; fall through
      }
      return body == null || body.isBlank() ? ex.getStatusText() : body;
   }

   /**
     * Runs a call, turning connection problems, timeouts and unexpected server errors into a
     * {@link DependencyUnavailableException}. Client errors (4xx) are passed to {@code onClientError}.
     */
   public static <T> T call(String dependency, Supplier<T> request, ClientErrorHandler onClientError) {
      try {
         return request.get();
      } catch (RestClientResponseException ex) {
         int status = ex.getStatusCode().value();
         if (ex.getStatusCode().is4xxClientError()) {
            throw onClientError.handle(status, reason(ex));
         }
         throw DependencyUnavailableException.of(dependency, "HTTP " + status, ex);
      } catch (RestClientException ex) {
         throw DependencyUnavailableException.of(dependency, ex.getMessage(), ex);
      }
   }

   @FunctionalInterface
   public interface ClientErrorHandler {
      RuntimeException handle(int status, String reason);
   }
}
