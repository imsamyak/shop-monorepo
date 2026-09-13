package com.ecom.catalog.domain.entity;

import java.math.BigDecimal;
import java.util.Map;

/**
 * A live SKU of a product as the home page shows it. {@code availableStock} is kept for deriving the availability; the
 * API only exposes {@link #availability()}.
 */
public record SkuSummary(String skuId, BigDecimal price, Map<String, String> properties, int availableStock) {

   public Availability availability() {
      return availableStock > 0 ? Availability.AVAILABLE : Availability.OUT_OF_STOCK;
   }
}
