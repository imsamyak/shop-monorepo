package com.ecom.listing.port.in.usecase.sku.dto.query;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Bulk lookup of SKUs by id, for services that need the current price and seller (checkout prices a cart with it).
 * SKUs that do not exist or were deleted are left out of the result instead of failing the call.
 */
public record LookupSkusQuery(
   @NotEmpty @Size(max = 100) List<@NotNull Long> skuIds
) {
}
