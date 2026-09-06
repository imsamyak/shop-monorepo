package com.ecom.listing.port.in.usecase.product.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

/**
 * One page of products. The listing is offset-paged, so the {@link #cursor()} is simply the next page index, or
 * {@code null} on the last page.
 */
public record ProductPageResult(
   List<ProductResult> items,
   String cursor,
   int page,
   int size,
   long totalElements,
   int totalPages
) implements Result.Page<ProductResult> {

   public ProductPageResult {
      items = items == null ? List.of() : List.copyOf(items);
   }
}
