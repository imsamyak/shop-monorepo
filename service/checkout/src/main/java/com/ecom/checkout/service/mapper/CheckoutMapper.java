package com.ecom.checkout.service.mapper;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutItem;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutItemResult;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckoutMapper {

   public CheckoutResult toResult(Checkout checkout) {
      if (checkout == null) {
         return null;
      }
      List<CheckoutItemResult> itemResults = checkout.getItems().stream().map(this::toItemResult).toList();
      return CheckoutResult.builder()
            .checkoutId(checkout.getId())
            .customerId(checkout.getCustomerId())
            .status(checkout.getStatus())
            .totalAmount(checkout.getTotalAmount())
            .expiresAt(checkout.getExpiresAt())
            .version(checkout.version())
            .items(itemResults)
            .build();
   }

   public CheckoutItemResult toItemResult(CheckoutItem item) {
      if (item == null) {
         return null;
      }
      return CheckoutItemResult.builder()
            .id(item.getId())
            .skuId(item.getSkuId())
            .sellerId(item.getSellerId())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .subtotal(item.getSubtotal())
            .reservationId(item.getReservationId())
            .build();
   }
}
