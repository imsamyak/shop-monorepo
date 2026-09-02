package com.ecom.checkout.adapter.out.client.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.ecom.checkout.adapter.out.client.ClientSupport;
import com.ecom.checkout.domain.exception.StockOperationRejectedException;
import com.ecom.checkout.port.out.client.catalog.LoadSkuPricePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads SKU prices and sellers from the product catalog read model (product-query).
 */
@Component
public class ProductQueryClientAdapter implements LoadSkuPricePort {

   private static final String DEPENDENCY = "product-query-service";

   private final RestClient restClient;

   public ProductQueryClientAdapter(
         @Value("${services.product-query.base-url}") String baseUrl,
         @Value("${services.product-query.connect-timeout-ms:2000}") int connectTimeoutMs,
         @Value("${services.product-query.read-timeout-ms:5000}") int readTimeoutMs) {
      this.restClient = ClientSupport.restClient(baseUrl, connectTimeoutMs, readTimeoutMs);
   }

   @Override
   public Map<Long, SkuPrice> loadPrices(Collection<Long> skuIds) {
      String ids = skuIds.stream().map(String::valueOf).collect(Collectors.joining(","));

      String body = ClientSupport.call(DEPENDENCY,
            () -> restClient.get()
                  .uri(uri -> uri.path("/products/items/lookup").queryParam("skuIds", ids).build())
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
