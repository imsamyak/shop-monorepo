package com.ecom.inventory.adapter.in.web.controller.item.dto.request;

import com.ecom.inventory.port.in.usecase.item.dto.command.AdjustStockCmd;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AdjustStockRequest(
      @NotNull(message = "Total quantity is required")
      @PositiveOrZero(message = "Total quantity cannot be negative")
      Integer totalQuantity
) {
   public AdjustStockCmd toCommand(Long skuId) {
      return new AdjustStockCmd(skuId, totalQuantity);
   }
}
