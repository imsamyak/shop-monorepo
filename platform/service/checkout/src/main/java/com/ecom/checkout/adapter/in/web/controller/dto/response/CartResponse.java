package com.ecom.checkout.adapter.in.web.controller.dto.response;

import com.ecom.checkout.port.in.usecase.dto.result.CartItemResult;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
      UUID customerId,
      List<CartItemResponse> items,
      long version,
      Instant updatedAt
) {
   public static CartResponse from(CartResult result) {
      return new CartResponse(
            result.customerId(),
            result.items().stream().map(CartItemResponse::from).toList(),
            result.version(),
            result.updatedAt());
   }

   public record CartItemResponse(Long skuId, int quantity) {
      public static CartItemResponse from(CartItemResult item) {
         return new CartItemResponse(item.skuId(), item.quantity());
      }
   }
}
