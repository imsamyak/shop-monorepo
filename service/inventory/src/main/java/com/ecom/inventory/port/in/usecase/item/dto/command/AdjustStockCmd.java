package com.ecom.inventory.port.in.usecase.item.dto.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Builder
public record AdjustStockCmd(
      @NotNull Long skuId,
      @PositiveOrZero int totalQuantity
) {
}
