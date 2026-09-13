package com.ecom.listing.port.in.usecase.product.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetProductQuery(@NotNull UUID id) {
}
