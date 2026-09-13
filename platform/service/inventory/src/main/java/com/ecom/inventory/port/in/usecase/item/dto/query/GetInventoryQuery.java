package com.ecom.inventory.port.in.usecase.item.dto.query;

import jakarta.validation.constraints.NotNull;

public record GetInventoryQuery(@NotNull Long skuId) {
}
