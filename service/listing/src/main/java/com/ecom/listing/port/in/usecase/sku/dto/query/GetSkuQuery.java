package com.ecom.listing.port.in.usecase.sku.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetSkuQuery(@NotNull UUID productId, @NotNull Long skuId) {
}
