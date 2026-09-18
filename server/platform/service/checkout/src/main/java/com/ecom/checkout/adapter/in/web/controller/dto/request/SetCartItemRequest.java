package com.ecom.checkout.adapter.in.web.controller.dto.request;

import com.ecom.checkout.port.in.usecase.dto.command.SetCartItemCmd;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SetCartItemRequest(
      @NotNull(message = "Quantity is required")
      @Positive(message = "Quantity must be positive")
      @Max(value = 1000, message = "Quantity is too large")
      Integer quantity,

      Long version
) {
   public SetCartItemCmd toCommand(UUID customerId, Long skuId) {
      return new SetCartItemCmd(customerId, skuId, quantity, version);
   }
}
