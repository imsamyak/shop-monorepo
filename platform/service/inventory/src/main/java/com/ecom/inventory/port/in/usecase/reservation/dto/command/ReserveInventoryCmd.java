package com.ecom.inventory.port.in.usecase.reservation.dto.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Reserves several SKUs atomically: either every line is reserved or none is.
 *
 * @param referenceId idempotency key (the checkout id). Repeating a request with the same reference returns the
 *                    reservations created the first time instead of reserving again.
 */
@Builder
public record ReserveInventoryCmd(
      @NotBlank @Size(max = 100) String referenceId,
      @Positive @Max(value = 86_400, message = "ttlSeconds cannot exceed 24 hours") long ttlSeconds,
      @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> items
) {

   @Builder
   public record Item(
         @NotNull Long skuId,
         @Positive @Max(10_000) int quantity
   ) {
   }
}
