package com.ecom.listing.adapter.in.web.controller.product.dto.response;

import com.ecom.listing.port.in.usecase.product.dto.result.ProductPageResult;

import java.util.List;

/**
 * @param nextPage the index of the next page, or {@code null} on the last page
 */
public record ProductPageResponse(
   List<ProductResponse> items,
   int page,
   int size,
   long totalElements,
   int totalPages,
   Integer nextPage
) {
   public static ProductPageResponse from(ProductPageResult result) {
      return new ProductPageResponse(
         result.items().stream().map(ProductResponse::from).toList(),
         result.page(),
         result.size(),
         result.totalElements(),
         result.totalPages(),
         result.cursor() == null ? null : Integer.valueOf(result.cursor())
      );
   }
}
