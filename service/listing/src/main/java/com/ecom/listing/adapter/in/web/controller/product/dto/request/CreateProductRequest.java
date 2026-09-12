package com.ecom.listing.adapter.in.web.controller.product.dto.request;

import com.ecom.listing.port.in.usecase.product.dto.command.CreateProductCmd;
import com.ecom.shared.validation.MonetaryAmount;
import com.ecom.shared.validation.RequiredText;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The seller is not part of the request: the product belongs to the seller in the access token. {@code images} are
 * the addresses returned by the asset service, in the order they should be shown (the first is the main picture).
 */
public record CreateProductRequest(
   @RequiredText(min = 5, max = 50) String title,
   @RequiredText(max = 200) String description,
   @NotNull @MonetaryAmount(min = "1.00") BigDecimal defaultPrice,
   List<String> images
) {

   public CreateProductCmd toCommand(UUID sellerId) {
      return new CreateProductCmd(this.title, this.description, this.defaultPrice, sellerId, this.images);
   }
}
