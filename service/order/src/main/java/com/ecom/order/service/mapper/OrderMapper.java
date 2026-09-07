package com.ecom.order.service.mapper;

import com.ecom.order.domain.entity.Order;
import com.ecom.order.domain.entity.OrderItem;
import com.ecom.order.port.in.usecase.dto.result.OrderItemResult;
import com.ecom.order.port.in.usecase.dto.result.OrderResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

   public OrderResult toResult(Order order) {
      if (order == null) {
         return null;
      }
      List<OrderItemResult> itemResults = order.getItems().stream().map(this::toItemResult).toList();
      return OrderResult.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .appliedVersion(order.getAppliedVersion())
            .items(itemResults)
            .build();
   }

   public OrderItemResult toItemResult(OrderItem item) {
      if (item == null) {
         return null;
      }
      return OrderItemResult.builder()
            .id(item.getId())
            .skuId(item.getSkuId())
            .sellerId(item.getSellerId())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .subtotal(item.getSubtotal())
            .build();
   }
}
