package com.ecom.listing.port.in.usecase.product.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * @param sellerId only products of this seller, or {@code null} for all
 * @param title    case-insensitive substring of the title, or {@code null} for all
 * @param page     zero-based page index
 */
@Builder
public record ListProductsQuery(
   UUID sellerId,
   String title,
   @Min(0) int page,
   @Min(1) @Max(ListProductsQuery.MAX_SIZE) int size,
   @NotNull ProductSort sort
) {

   public static final int MAX_SIZE = 100;
}
