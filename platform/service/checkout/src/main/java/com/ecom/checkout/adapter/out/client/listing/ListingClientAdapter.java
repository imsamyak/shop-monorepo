package com.ecom.checkout.adapter.out.client.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.ecom.checkout.adapter.out.client.ClientSupport;
import com.ecom.checkout.adapter.out.client.identity.ServiceTokenProvider;
import com.ecom.checkout.domain.exception.StockOperationRejectedException;
import com.ecom.checkout.port.out.client.catalog.LoadSkuPricePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads SKU prices and sellers from the listing service, the source of truth, so a customer is always charged the
 * current price.
 */
@Component
public class ListingClientAdapter implements LoadSkuPricePort {

   private static final String DEPENDENCY = "listing-service";

   private final RestClient restClient;
   private final ServiceTokenProvider tokens;

   public ListingClientAdapter(
         ServiceTokenProvider tokens,
         @Value("${services.listing.base-url}") String baseUrl,
         @Value("${services.listing.connect-timeout-ms:2000}") int connectTimeoutMs,
         @Value("${services.listing.read-timeout-ms:5000}") int readTimeoutMs) {
      this.restClient = ClientSupport.restClient(baseUrl, connectTimeoutMs, readTimeoutMs);
      this.tokens = tokens;
   }

   @Override
   public Map<Long, SkuPrice> loadPrices(Collection<Long> skuIds) {
      String ids = skuIds.stream().map(String::valueOf).collect(Collectors.joining(","));

      String body = ClientSupport.call(DEPENDENCY,
            () -> restClient.get()
                  .uri(uri -> uri.path("/skus").queryParam("skuIds", ids).build())
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token())
                  .retrieve()
                  .body(String.class),
            (status, reason) -> StockOperationRejectedException.of("look up prices", reason));

      Map<Long, SkuPrice> prices = new HashMap<>();
      for (JsonNode node : ClientSupport.parse("{\"items\":" + (body == null || body.isBlank() ? "[]" : body) + "}").path("items")) {
         long skuId = node.path("skuId").asLong();
         prices.put(skuId, new SkuPrice(
               skuId,
               UUID.fromString(node.path("productId").asText()),
               UUID.fromString(node.path("sellerId").asText()),
               new BigDecimal(node.path("price").asText())));
      }
      return prices;
   }
}
