package com.ecom.catalog.port.in.usecase.dto;

import jakarta.validation.constraints.NotBlank;

public record GetProductQuery(@NotBlank String productId) {
}
