package com.ecom.listing.port.in.usecase.sku.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DeleteSkuCmd(
   @NotNull UUID productId,
   @NotNull Long skuId,
   long version
) {
}
