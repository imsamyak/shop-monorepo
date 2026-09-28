package com.ecom.order.adapter.in.web.controller.dto.response;

import com.ecom.order.domain.entity.OrderStatus;
import com.ecom.order.port.in.usecase.dto.result.OrderItemResult;
import com.ecom.order.port.in.usecase.dto.result.OrderResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
      UUID orderId,
      UUID customerId,
      OrderStatus status,
      BigDecimal totalAmount,
      long version,
      Instant createdAt,
      List<OrderItemResponse> items
) {
   public static OrderResponse from(OrderResult result) {
      if (result == null) {
         return null;
      }
      List<OrderItemResponse> itemResponses = result.items() != null
            ? result.items().stream().map(OrderItemResponse::from).toList()
            : List.of();
      return new OrderResponse(
            result.orderId(),
            result.customerId(),
            result.status(),
            result.totalAmount(),
            result.appliedVersion(),
            result.createdAt(),
            itemResponses);
   }

   public record OrderItemResponse(
         Long id,
         Long skuId,
         UUID sellerId,
         int quantity,
         BigDecimal unitPrice,
         BigDecimal subtotal
   ) {
      public static OrderItemResponse from(OrderItemResult item) {
         return new OrderItemResponse(
               item.id(),
               item.skuId(),
               item.sellerId(),
               item.quantity(),
               item.unitPrice(),
               item.subtotal());
      }
   }
}
