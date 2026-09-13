package com.ecom.checkout.port.in.usecase.dto.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * @param expectedVersion the cart version the caller last saw; when given, a different current version is a conflict
 */
public record SetCartItemCmd(
      @NotNull UUID customerId,
      @NotNull Long skuId,
      @Positive @Max(1000) int quantity,
      Long expectedVersion
) {
}
