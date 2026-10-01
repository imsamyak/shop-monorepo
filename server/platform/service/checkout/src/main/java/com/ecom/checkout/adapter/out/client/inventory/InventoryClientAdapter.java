package com.ecom.checkout.adapter.out.client.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.ecom.checkout.adapter.out.client.ClientSupport;
import com.ecom.checkout.adapter.out.client.identity.ServiceTokenProvider;
import com.ecom.checkout.domain.exception.StockOperationRejectedException;
import com.ecom.checkout.port.out.client.inventory.CommitStockPort;
import com.ecom.checkout.port.out.client.inventory.ReleaseStockPort;
import com.ecom.checkout.port.out.client.inventory.ReserveStockPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls inventory over HTTP, signed in as this service (inventory only accepts reservation calls from services).
 * All calls have timeouts and map failures: rejections by inventory become {@link StockOperationRejectedException}
 * (422), everything else a 503.
 */
@Component
public class InventoryClientAdapter implements ReserveStockPort, CommitStockPort, ReleaseStockPort {

   private static final String DEPENDENCY = "inventory-service";

   private final RestClient restClient;
   private final ServiceTokenProvider tokens;

   public InventoryClientAdapter(
         ServiceTokenProvider tokens,
         @Value("${services.inventory.base-url}") String baseUrl,
         @Value("${services.inventory.connect-timeout-ms:2000}") int connectTimeoutMs,
         @Value("${services.inventory.read-timeout-ms:5000}") int readTimeoutMs) {
      this.restClient = ClientSupport.restClient(baseUrl, connectTimeoutMs, readTimeoutMs);
      this.tokens = tokens;
   }

   @Override
   public List<ReservedStock> reserve(String referenceId, long ttlSeconds, List<StockLine> lines) {
      Map<String, Object> request = Map.of(
            "referenceId", referenceId,
            "ttlSeconds", ttlSeconds,
            "items", lines.stream().map(l -> Map.of("skuId", l.skuId(), "quantity", l.quantity())).toList());

      String body = post("/api/inventory/reservations", request, "reserve stock");
      List<ReservedStock> reserved = new ArrayList<>();
      for (JsonNode node : ClientSupport.parse(body).path("reservations")) {
         reserved.add(new ReservedStock(node.path("skuId").asLong(), UUID.fromString(node.path("reservationId").asText())));
      }
      return reserved;
   }

   @Override
   public void commit(Collection<UUID> reservationIds) {
      post("/api/inventory/reservations/commit", Map.of("reservationIds", reservationIds), "commit stock");
   }

   @Override
   public void release(Collection<UUID> reservationIds) {
      post("/api/inventory/reservations/release", Map.of("reservationIds", reservationIds), "release stock");
   }

   private String post(String path, Object request, String operation) {
      return ClientSupport.call(DEPENDENCY,
            () -> restClient.post()
                  .uri(path)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token())
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(request)
                  .retrieve()
                  .body(String.class),
            (status, reason) -> StockOperationRejectedException.of(operation, reason));
   }
}
