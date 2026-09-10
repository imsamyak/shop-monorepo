package com.ecom.order.adapter.in.web.controller;

import com.ecom.order.adapter.in.web.controller.dto.response.OrderResponse;
import com.ecom.order.adapter.in.web.controller.dto.response.OrdersResponse;
import com.ecom.order.port.in.usecase.GetOrderUseCase;
import com.ecom.order.port.in.usecase.ListOrdersUseCase;
import com.ecom.order.port.in.usecase.dto.query.GetOrderQuery;
import com.ecom.order.port.in.usecase.dto.query.ListOrdersQuery;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only: orders are built from checkout events, never written through the API. A customer sees their own orders;
 * an admin sees everyone's.
 */
@RestController
@RequestMapping("/orders")
@RequireRole({UserRole.ROLE_BUYER, UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class OrderController {

   private final GetOrderUseCase getOrderUseCase;
   private final ListOrdersUseCase listOrdersUseCase;

   @GetMapping("/{orderId}")
   public ResponseEntity<OrderResponse> get(@PathVariable UUID orderId) {
      return ResponseEntity.ok(OrderResponse.from(getOrderUseCase.execute(new GetOrderQuery(orderId))));
   }

   @GetMapping
   public ResponseEntity<OrdersResponse> list(@Valid ListOrdersQuery query) {
      return ResponseEntity.ok(OrdersResponse.from(listOrdersUseCase.execute(query)));
   }
}
