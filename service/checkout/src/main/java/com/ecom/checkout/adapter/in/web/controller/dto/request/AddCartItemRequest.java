package com.ecom.checkout.adapter.in.web.controller.dto.request;

import com.ecom.checkout.port.in.usecase.dto.command.AddCartItemCmd;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequest(
      @NotNull(message = "SKU ID is required")
      Long skuId,

      @NotNull(message = "Quantity is required")
      @Positive(message = "Quantity must be positive")
      @Max(value = 1000, message = "Quantity is too large")
      Integer quantity,

      Long version
) {
   public AddCartItemCmd toCommand(UUID customerId) {
      return new AddCartItemCmd(customerId, skuId, quantity, version);
   }
}
