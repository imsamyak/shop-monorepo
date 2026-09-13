package com.ecom.order.service.consumer;

import com.ecom.contract.checkout.Checkout;
import com.ecom.order.domain.entity.OrderItem;

import java.util.List;
import java.util.UUID;

/**
 * Reads the checkout events' string ids and items into the order's own types.
 */
final class CheckoutEvents {

   private CheckoutEvents() {
   }

   static UUID orderId(Checkout event) {
      return UUID.fromString(event.checkoutId());
   }

   static UUID customerId(String userId) {
      return userId == null ? null : UUID.fromString(userId);
   }

   static List<OrderItem> items(List<Checkout.Item> items) {
      if (items == null) {
         return List.of();
      }
      return items.stream()
            .map(i -> OrderItem.of(Long.parseLong(i.skuId()), UUID.fromString(i.sellerId()), i.quantity(), i.unitPrice()))
            .toList();
   }
}
