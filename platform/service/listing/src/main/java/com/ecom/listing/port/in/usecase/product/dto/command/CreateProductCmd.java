package com.ecom.listing.port.in.usecase.product.dto.command;

import com.ecom.shared.validation.MonetaryAmount;
import com.ecom.shared.validation.RequiredText;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param sellerId the seller who owns the product
 * @param images   the picture addresses in display order, or null for none
 */
@Builder
public record CreateProductCmd(
   @RequiredText(min = 5, max = 50) String title,
   @RequiredText(max = 200) String description,
   @NotNull @MonetaryAmount(min = "1.00") BigDecimal defaultPrice,
   @NotNull UUID sellerId,
   List<String> images
) {
}
