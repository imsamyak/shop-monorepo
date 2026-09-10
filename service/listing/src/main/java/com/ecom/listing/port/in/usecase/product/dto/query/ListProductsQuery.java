package com.ecom.listing.port.in.usecase.product.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.UUID;

/**
 * A page of products. The web layer binds the query string straight into this record, so the defaults and limits
 * live here.
 *
 * @param sellerId only products of this seller, or {@code null} for all
 * @param title    case-insensitive substring of the title, or {@code null} for all
 * @param page     zero-based page index, 0 when not given
 * @param size     page size, 20 when not given, at most {@value MAX_SIZE}
 * @param sort     the ordering, newest first when not given
 */
@Builder
public record ListProductsQuery(
   UUID sellerId,
   String title,
   @Min(0) Integer page,
   @Min(1) @Max(ListProductsQuery.MAX_SIZE) Integer size,
   ProductSort sort
) {
   public static final int MAX_SIZE = 100;
   public static final int DEFAULT_SIZE = 20;

   public ListProductsQuery {
      page = page == null ? 0 : page;
      size = size == null ? DEFAULT_SIZE : size;
      sort = sort == null ? ProductSort.CREATED_AT_DESC : sort;
   }
}
