package com.ecom.listing.adapter.in.web.controller.sku.dto.request;

import com.ecom.listing.port.in.usecase.sku.dto.command.UpdateSkuCmd;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record UpdateSkuRequest(
   long version,
   @DecimalMin(value = "0.00", inclusive = true, message = "Price cannot be negative")
   BigDecimal price,
   PropertyUpdatesDto properties
) {
   public record PropertyUpdatesDto(
      Map<String, String> add,
      Set<String> remove
   ) {
      public UpdateSkuCmd.PropertyUpdates toCommand() {
         return new UpdateSkuCmd.PropertyUpdates(add, remove);
      }
   }

   public UpdateSkuCmd toCommand(UUID productId, Long skuId) {
      return new UpdateSkuCmd(
         productId,
         skuId,
         version,
         price,
         properties != null ? properties.toCommand() : null
      );
   }
}
