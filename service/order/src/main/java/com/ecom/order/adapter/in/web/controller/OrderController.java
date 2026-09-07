package com.ecom.order.adapter.in.web.controller;

import com.ecom.order.adapter.in.web.controller.dto.response.OrderResponse;
import com.ecom.order.adapter.in.web.controller.dto.response.OrdersResponse;
import com.ecom.order.domain.entity.OrderStatus;
import com.ecom.order.port.in.usecase.GetOrderUseCase;
import com.ecom.order.port.in.usecase.ListOrdersUseCase;
import com.ecom.order.port.in.usecase.dto.query.GetOrderQuery;
import com.ecom.order.port.in.usecase.dto.query.ListOrdersQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only: orders are built from checkout events, never written through the API. Nothing here checks who is calling.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

   private final GetOrderUseCase getOrderUseCase;
   private final ListOrdersUseCase listOrdersUseCase;

   @GetMapping("/{orderId}")
   public ResponseEntity<OrderResponse> get(@PathVariable UUID orderId) {
      return ResponseEntity.ok(OrderResponse.from(getOrderUseCase.execute(new GetOrderQuery(orderId))));
   }

   @GetMapping
   public ResponseEntity<OrdersResponse> list(@RequestParam(required = false) UUID customerId,
                                              @RequestParam(required = false) OrderStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
      return ResponseEntity.ok(OrdersResponse.from(
            listOrdersUseCase.execute(new ListOrdersQuery(customerId, status, page, size))));
   }
}
