package com.ecom.inventory.port.in.usecase.item.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Stock of many SKUs. The web layer binds the query string straight into this record, so the defaults and limits
 * live here.
 *
 * @param skuIds when given (1 to 100), a lookup of exactly these SKUs; without it the whole stock is listed
 * @param page   zero-based page number, 0 when not given
 * @param size   items per page, 20 when not given; the listing is sorted by sku id
 */
public record ListInventoryQuery(
      @Size(max = 100) List<@NotNull Long> skuIds,
      @Min(0) Integer page,
      @Min(1) @Max(100) Integer size
) {
   public ListInventoryQuery {
      skuIds = skuIds == null ? null : List.copyOf(skuIds);
      page = page == null ? 0 : page;
      size = size == null ? 20 : size;
   }

   /** {@code skuIds=} given, even if empty, asks for a lookup (and an empty one is rejected there). */
   public boolean isLookup() {
      return skuIds != null;
   }
}
