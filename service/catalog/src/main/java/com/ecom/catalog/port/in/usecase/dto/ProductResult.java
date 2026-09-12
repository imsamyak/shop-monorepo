package com.ecom.catalog.port.in.usecase.dto;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.shared.dto.result.Result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A product as the home page shows it. There are no quantities, only {@link Availability}.
 *
 * @param images         the picture addresses in display order, the first being the main one
 * @param price          the product's default price
 * @param lowestSkuPrice the cheapest SKU, null while the product has no SKU
 */
public record ProductResult(
      String id,
      String sellerId,
      String title,
      String description,
      List<String> images,
      BigDecimal price,
      BigDecimal lowestSkuPrice,
      Availability availability,
      Instant createdAt,
      List<SkuResult> skus
) implements Result.One {

   public record SkuResult(String skuId, BigDecimal price, Map<String, String> properties, Availability availability) {
   }
}
