package com.ecom.checkout.adapter.in.web.controller.dto.response;

import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutItemResult;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
      UUID checkoutId,
      UUID customerId,
      CheckoutStatus status,
      BigDecimal totalAmount,
      Instant expiresAt,
      long version,
      List<CheckoutItemResponse> items
) {
   public static CheckoutResponse from(CheckoutResult result) {
      if (result == null) {
         return null;
      }
      List<CheckoutItemResponse> itemResponses = result.items() != null
            ? result.items().stream().map(CheckoutItemResponse::from).toList()
            : List.of();
      return new CheckoutResponse(
            result.checkoutId(),
            result.customerId(),
            result.status(),
            result.totalAmount(),
            result.expiresAt(),
            result.version(),
            itemResponses);
   }

   public record CheckoutItemResponse(
         Long id,
         Long skuId,
         UUID sellerId,
         int quantity,
         BigDecimal unitPrice,
         BigDecimal subtotal
   ) {
      public static CheckoutItemResponse from(CheckoutItemResult item) {
         return new CheckoutItemResponse(
               item.id(),
               item.skuId(),
               item.sellerId(),
               item.quantity(),
               item.unitPrice(),
               item.subtotal());
      }
   }
}
