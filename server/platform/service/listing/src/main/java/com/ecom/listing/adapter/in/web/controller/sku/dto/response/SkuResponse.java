package com.ecom.listing.adapter.in.web.controller.sku.dto.response;

import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SkuResponse(
   Long skuId,
   UUID productId,
   UUID sellerId,
   BigDecimal price,
   Map<String, String> properties,
   long version
) {
   public static SkuResponse from(SkuResult result) {
      return new SkuResponse(
         result.skuId(),
         result.productId(),
         result.sellerId(),
         result.price(),
         result.properties(),
         result.version()
      );
   }
}
