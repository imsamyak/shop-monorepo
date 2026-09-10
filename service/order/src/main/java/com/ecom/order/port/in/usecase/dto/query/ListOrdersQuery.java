package com.ecom.order.port.in.usecase.dto.query;

import com.ecom.order.domain.entity.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

/**
 * A page of orders. The web layer binds the query string straight into this record, so the defaults and limits live
 * here.
 *
 * @param customerId only this customer's orders; null for the caller's own (everyone's for an admin)
 * @param status     only orders in this status; null for any
 * @param page       zero-based page number, 0 when not given
 * @param size       orders per page, 20 when not given, at most {@value #MAX_SIZE}
 */
public record ListOrdersQuery(
      UUID customerId,
      OrderStatus status,
      @Min(0) Integer page,
      @Min(1) @Max(MAX_SIZE) Integer size
) {
   public static final int MAX_SIZE = 100;
   public static final int DEFAULT_SIZE = 20;

   public ListOrdersQuery {
      page = page == null ? 0 : page;
      size = size == null ? DEFAULT_SIZE : size;
   }
}
