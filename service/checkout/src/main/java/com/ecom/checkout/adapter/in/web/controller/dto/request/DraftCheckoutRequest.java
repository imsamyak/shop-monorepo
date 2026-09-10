package com.ecom.checkout.adapter.in.web.controller.dto.request;

import com.ecom.checkout.port.in.usecase.dto.command.DraftCheckoutCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * A draft is described by the SKUs and quantities only. The customer is the caller in the access token, and prices and
 * sellers are looked up server-side, so the client cannot supply (or forge) them. Leaving the items out (or empty)
 * drafts the customer's current cart.
 */
public record DraftCheckoutRequest(
      @Size(max = 50, message = "A checkout can contain at most 50 different items")
      List<@NotNull @Valid ItemRequest> items,

      @Positive(message = "ttlSeconds must be positive")
      @Max(value = 3600, message = "ttlSeconds cannot exceed one hour")
      Long ttlSeconds
) {
   public DraftCheckoutCmd toCommand(UUID customerId) {
      return new DraftCheckoutCmd(
            customerId,
            items == null ? List.of() : items.stream().map(i -> new DraftCheckoutCmd.ItemCmd(i.skuId(), i.quantity())).toList(),
            ttlSeconds != null ? ttlSeconds : DraftCheckoutCmd.DEFAULT_TTL_SECONDS);
   }

   public record ItemRequest(
         @NotNull(message = "SKU ID is required")
         Long skuId,

         @NotNull(message = "Quantity is required")
         @Positive(message = "Quantity must be positive")
         @Max(value = 1000, message = "Quantity is too large")
         Integer quantity
   ) {
   }
}
