package com.ecom.catalog.port.in.usecase.dto;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.ProductSort;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * A page of the home page feed. The web layer binds the query string straight into this record, so the defaults and
 * limits live here.
 *
 * @param page         zero-based, 0 when not given
 * @param size         products per page, 20 when not given, at most {@value #MAX_SIZE}
 * @param sort         the order, newest first when not given
 * @param availability only products with this availability, or null for all
 */
public record ListProductsQuery(
      @Min(0) Integer page,
      @Min(1) @Max(MAX_SIZE) Integer size,
      ProductSort sort,
      Availability availability
) {
   public static final int MAX_SIZE = 100;
   public static final int DEFAULT_SIZE = 20;

   public ListProductsQuery {
      page = page == null ? 0 : page;
      size = size == null ? DEFAULT_SIZE : size;
      sort = sort == null ? ProductSort.NEWEST : sort;
   }
}
