package com.ecom.listing.port.in.usecase.sku.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Builder
public record UpdateSkuCmd(
    @NotNull UUID productId,
    @NotNull Long skuId,
    long version,
    BigDecimal price,
    PropertyUpdates properties
) {

   public UpdateSkuCmd {
      properties = properties != null && properties.hasChanges() ?
          properties : null;
   }

   @Builder
   public record PropertyUpdates(
       Map<
           @NotBlank @Size(max = 50) @Pattern(regexp = "[^,:]*", message = "Property names cannot contain ',' or ':'") String,
           @NotNull @NotBlank @Size(max = 100) @Pattern(regexp = "[^,]*", message = "Property values cannot contain ','") String
       > add,
       Set<@NotNull @NotBlank String> remove
   ) {
      public PropertyUpdates {
         add = add != null ? Map.copyOf(add) : Map.of();
         remove = remove != null ? Set.copyOf(remove) : Set.of();

         boolean collision = add.keySet().stream().anyMatch(remove::contains);
         if (collision) {
            throw new IllegalArgumentException("Cannot add and remove the same property key in the same command");
         }
      }

      public boolean hasChanges() {
         return !add.isEmpty() || !remove.isEmpty();
      }
   }
}
