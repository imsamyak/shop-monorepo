package com.ecom.listing.adapter.in.web.controller.product.dto.response;

import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
   UUID id,
   UUID sellerId,
   String title,
   String description,
   BigDecimal defaultPrice,
   List<String> images,
   long version,
   List<Long> deletedSkuIds
) {
   public static ProductResponse from(ProductResult result) {
      if (result == null) {
         return null;
      }
      return new ProductResponse(
         result.id(),
         result.sellerId(),
         result.title(),
         result.description(),
         result.defaultPrice(),
         result.images(),
         result.version(),
         result.deletedSkuIds()
      );
   }
}
