package com.ecom.contract.product;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Domain events emitted by the Product boundary for Product lifecycle mutations.
 * <p>
 * {@code version} is the product's version after the change that caused the event. It only grows, so a consumer that
 * keeps the highest version it has applied can ignore duplicates and late older events. {@code Deleted} is final: a
 * consumer applies it whatever version it carries.
 */
public sealed interface Product extends DomainEvent {

   String productId();

   @Override
   default String aggregateId() {
      return productId();
   }

   @Builder
   record Created(String productId, String sellerId, String title, String description, BigDecimal defaultPrice, long version) implements Product {
   }

   @Builder
   record Updated(String productId, String title, String description, long version) implements Product {
   }

   @Builder
   record Deleted(String productId, long version) implements Product {
   }
}
