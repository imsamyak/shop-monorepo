package com.ecom.listing.adapter.in.web.controller.product.dto.request;

import com.ecom.listing.port.in.usecase.product.dto.command.CreateProductCmd;
import com.ecom.shared.validation.MonetaryAmount;
import com.ecom.shared.validation.RequiredText;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param sellerId the seller who owns the product
 */
public record CreateProductRequest(
   @RequiredText(min = 5, max = 50) String title,
   @RequiredText(max = 200) String description,
   @NotNull @MonetaryAmount(min = "1.00") BigDecimal defaultPrice,
   @NotNull UUID sellerId
) {

   public CreateProductCmd toCommand() {
      return new CreateProductCmd(this.title, this.description, this.defaultPrice, this.sellerId);
   }
}
