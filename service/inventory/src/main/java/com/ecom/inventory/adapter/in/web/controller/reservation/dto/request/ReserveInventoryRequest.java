package com.ecom.inventory.adapter.in.web.controller.reservation.dto.request;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReserveInventoryCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReserveInventoryRequest(
      @NotBlank(message = "referenceId is required")
      @Size(max = 100)
      String referenceId,

      @NotNull(message = "ttlSeconds is required")
      @Positive(message = "ttlSeconds must be positive")
      @Max(value = 86_400, message = "ttlSeconds cannot exceed 24 hours")
      Long ttlSeconds,

      @NotEmpty(message = "At least one item is required")
      @Size(max = 50, message = "At most 50 items per reservation")
      List<@NotNull @Valid Item> items
) {

   public record Item(
         @NotNull(message = "SKU ID is required") Long skuId,
         @NotNull(message = "Quantity is required") @Positive @Max(10_000) Integer quantity
   ) {
   }

   public ReserveInventoryCmd toCommand() {
      return new ReserveInventoryCmd(
            referenceId,
            ttlSeconds,
            items.stream().map(i -> new ReserveInventoryCmd.Item(i.skuId(), i.quantity())).toList());
   }
}
