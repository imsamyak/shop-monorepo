package com.ecom.inventory.port.in.usecase.item.dto.query;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bulk lookup by sku id. SKUs without an inventory item are left out of the result instead of failing the call.
 */
public record GetInventoriesQuery(
      @NotEmpty @Size(max = 100) List<@NotNull Long> skuIds
) {
}
