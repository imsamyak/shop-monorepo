package com.ecom.order.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetOrderQuery(@NotNull UUID orderId) {
}
