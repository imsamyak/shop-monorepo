package com.ecom.order.port.in.usecase.dto.query;

import com.ecom.order.domain.entity.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * @param customerId only this customer's orders; null for everyone's
 * @param status     only orders in this status; null for any
 * @param page       zero-based page number
 * @param size       orders per page, at most {@value #MAX_SIZE}
 */
public record ListOrdersQuery(
      UUID customerId,
      OrderStatus status,
      @Min(0) int page,
      @Min(1) @Max(MAX_SIZE) int size
) {
   public static final int MAX_SIZE = 100;
}
