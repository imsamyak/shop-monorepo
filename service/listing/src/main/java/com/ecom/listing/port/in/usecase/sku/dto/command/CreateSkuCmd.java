package com.ecom.listing.port.in.usecase.sku.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Property keys may not contain {@code ,} or {@code :}; values may not contain {@code ,} (they are stored as
 * {@code key:value,key:value}).
 */
@Builder
public record CreateSkuCmd(
   @NotNull UUID productId,
   Optional<BigDecimal> price,
   @Size(max = 20, message = "A SKU can have at most 20 properties")
   Map<
      @NotBlank @Size(max = 50) @Pattern(regexp = "[^,:]*", message = "Property names cannot contain ',' or ':'") String,
      @NotNull @NotBlank @Size(max = 100) @Pattern(regexp = "[^,]*", message = "Property values cannot contain ','") String
   > properties
) {
}
