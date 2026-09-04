package com.ecom.contract.checkout;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain events emitted by the Checkout boundary. {@code version} is the checkout's version after the change that
 * caused the event (see {@code Product}); a cancellation caused by expiry carries the version of the expiry.
 */
public sealed interface Checkout extends DomainEvent {

   String checkoutId();

   @Override
   default String aggregateId() {
      return checkoutId();
   }

   @Builder
   record Drafted(String checkoutId, String userId, List<Item> items, long version) implements Checkout {
   }

   @Builder
   record Committed(String checkoutId, String userId, List<Item> items, long version) implements Checkout {
   }

   @Builder
   record Cancelled(String checkoutId, String reason, long version) implements Checkout {
   }

   @Builder
   record Item(String skuId, int quantity, BigDecimal unitPrice, String sellerId) {
   }
}
