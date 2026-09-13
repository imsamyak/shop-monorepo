package com.ecom.listing.port.in.usecase.product.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DeleteProductCmd(
   @NotNull UUID id,
   long version
) {
}
