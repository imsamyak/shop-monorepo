package com.ecom.catalog.port.in.usecase.dto;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.ProductSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param page         zero-based
 * @param availability only products with this availability, or null for all
 */
public record ListProductsQuery(
      @Min(0) int page,
      @Min(1) @Max(MAX_SIZE) int size,
      @NotNull ProductSort sort,
      Availability availability
) {

   public static final int MAX_SIZE = 100;
}
