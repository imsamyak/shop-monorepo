package com.ecom.checkout.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @param expectedVersion the cart version the caller last saw; when given, a different current version is a conflict
 */
public record RemoveCartItemCmd(@NotNull UUID customerId, @NotNull Long skuId, Long expectedVersion) {
}
