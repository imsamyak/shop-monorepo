package com.ecom.checkout.port.in.usecase.dto.result;

import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.shared.dto.result.Result;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record CheckoutResult(
      UUID checkoutId,
      UUID customerId,
      CheckoutStatus status,
      BigDecimal totalAmount,
      Instant expiresAt,
      long version,
      List<CheckoutItemResult> items
) implements Result.One {
}
