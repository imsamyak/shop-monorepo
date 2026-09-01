package com.ecom.inventory.port.in.usecase.item.dto.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record RemoveStockCmd(
      @NotNull Long skuId,
      @Positive @Max(1_000_000) int quantity
) {
}
