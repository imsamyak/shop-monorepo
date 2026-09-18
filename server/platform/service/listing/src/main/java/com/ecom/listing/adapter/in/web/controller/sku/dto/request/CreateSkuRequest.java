package com.ecom.listing.adapter.in.web.controller.sku.dto.request;

import com.ecom.listing.port.in.usecase.sku.dto.command.CreateSkuCmd;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record CreateSkuRequest(
   @DecimalMin(value = "0.00", inclusive = true, message = "Price cannot be negative")
   BigDecimal price,
   Map<String, String> properties
) {
   public CreateSkuCmd toCommand(UUID productId) {
      return new CreateSkuCmd(
         productId,
         Optional.ofNullable(price),
         properties != null ? properties : Map.of()
      );
   }
}
