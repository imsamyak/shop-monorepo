package com.ecom.order.port.in.usecase.dto.result;

import com.ecom.order.domain.entity.OrderStatus;
import com.ecom.shared.dto.result.Result;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResult(
      UUID orderId,
      UUID customerId,
      OrderStatus status,
      BigDecimal totalAmount,
      long appliedVersion,
      Instant createdAt,
      List<OrderItemResult> items
) implements Result.One {
}
