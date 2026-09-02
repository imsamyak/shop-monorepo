package com.ecom.checkout.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetCheckoutQuery(@NotNull UUID checkoutId) {
}
