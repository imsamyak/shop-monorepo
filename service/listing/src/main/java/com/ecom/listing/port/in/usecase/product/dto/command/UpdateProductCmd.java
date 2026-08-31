package com.ecom.listing.port.in.usecase.product.dto.command;

import com.ecom.shared.dto.patch.Patch;
import com.ecom.shared.validation.OptionalText;
import com.ecom.shared.validation.RequiredText;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateProductCmd(
   @NotNull UUID id,
   long version,
   @RequiredText(min = 5, max = 50) String title,
   Patch<@OptionalText(max = 200) String> description
) {

   public UpdateProductCmd {
      description = description != null ? description : Patch.undefined();
   }
}
