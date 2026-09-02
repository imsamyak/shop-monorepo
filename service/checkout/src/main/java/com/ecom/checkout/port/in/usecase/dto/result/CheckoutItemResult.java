package com.ecom.checkout.port.in.usecase.dto.result;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CheckoutItemResult(
      Long id,
      Long skuId,
      UUID sellerId,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal,
      UUID reservationId
) {
}
