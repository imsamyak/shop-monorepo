package com.ecom.checkout.port.out.client.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Looks up what a SKU really costs and who sells it. Checkout never trusts a price supplied by the client.
 */
public interface LoadSkuPricePort {

   /** Prices for the SKUs that exist; unknown SKUs are absent from the result. */
   Map<Long, SkuPrice> loadPrices(Collection<Long> skuIds);

   record SkuPrice(Long skuId, UUID productId, UUID sellerId, BigDecimal price) {
   }
}
