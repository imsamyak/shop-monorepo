package com.ecom.order.adapter.in.web.controller.dto.response;

import com.ecom.order.port.in.usecase.dto.result.OrdersResult;

import java.util.List;

public record OrdersResponse(
      List<OrderResponse> items,
      int page,
      int size,
      long totalElements,
      int totalPages
) {
   public static OrdersResponse from(OrdersResult result) {
      return new OrdersResponse(
            result.items().stream().map(OrderResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages());
   }
}
