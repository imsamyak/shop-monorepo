package com.ecom.listing.adapter.in.web.controller.product.dto.request;

import com.ecom.listing.port.in.usecase.product.dto.command.UpdateProductCmd;
import com.ecom.shared.dto.patch.Patch;
import com.ecom.shared.validation.OptionalText;
import com.ecom.shared.validation.RequiredText;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.UUID;

public record UpdateProductRequest(
   long version,
   @RequiredText(min = 5, max = 50) String title,
   JsonNullable<@OptionalText(max = 200) String> description
) {

   public UpdateProductRequest {
      description = description != null ? description : JsonNullable.undefined();
   }

   public UpdateProductCmd toCommand(UUID id) {
      return new UpdateProductCmd(
         id,
         this.version,
         this.title,
         Patch.from(this.description)
      );
   }
}
