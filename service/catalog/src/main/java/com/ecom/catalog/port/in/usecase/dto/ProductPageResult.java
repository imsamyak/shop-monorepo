package com.ecom.catalog.port.in.usecase.dto;

import com.ecom.shared.dto.result.Result;

import java.util.List;

/**
 * One page of the home page feed.
 *
 * @param page zero-based number of this page
 */
public record ProductPageResult(
      List<ProductResult> items,
      int page,
      int size,
      long totalElements,
      int totalPages
) implements Result.Many<ProductResult> {
}
