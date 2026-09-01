package com.ecom.inventory.adapter.in.web.controller.item.dto.request;

import com.ecom.inventory.port.in.usecase.item.dto.command.RemoveStockCmd;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RemoveStockRequest(
      @NotNull(message = "Quantity is required")
      @Positive(message = "Quantity must be positive")
      @Max(value = 1_000_000, message = "Quantity is too large")
      Integer quantity
) {
   public RemoveStockCmd toCommand(Long skuId) {
      return new RemoveStockCmd(skuId, quantity);
   }
}
