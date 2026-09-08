package com.ecom.inventory.port.in.usecase.item.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param page zero-based page number
 * @param size items per page; the listing is sorted by sku id
 */
public record ListInventoryQuery(
      @Min(0) int page,
      @Min(1) @Max(100) int size
) {
}
