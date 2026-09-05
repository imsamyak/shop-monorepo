package com.ecom.checkout.port.in.usecase.dto.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Items are identified by SKU and quantity only: the price and the seller of each SKU come from the catalog, and the
 * customer is named in the command. Without items the draft is made from the customer's cart.
 */
@Builder
public record DraftCheckoutCmd(
      @NotNull UUID customerId,
      @Size(max = 50) List<@NotNull @Valid ItemCmd> items,
      @Max(value = 3600, message = "ttlSeconds cannot exceed one hour") long ttlSeconds
) {
   public static final long DEFAULT_TTL_SECONDS = 900;

   public DraftCheckoutCmd {
      if (items == null) {
         items = List.of();
      }
      if (ttlSeconds <= 0) {
         ttlSeconds = DEFAULT_TTL_SECONDS;
      }
   }

   @Builder
   public record ItemCmd(
         @NotNull Long skuId,
         @Positive @Max(1000) int quantity
   ) {
   }
}
