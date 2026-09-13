package com.ecom.contract.product;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain events emitted by the Product boundary for Product lifecycle mutations.
 * <p>
 * {@code version} is the product's version after the change that caused the event. It only grows, so a consumer that
 * keeps the highest version it has applied can ignore duplicates and late older events. {@code Deleted} is final: a
 * consumer applies it whatever version it carries.
 * <p>
 * {@code images} are the product's picture addresses in display order, the first being the main picture; every
 * {@code Created} and {@code Updated} carries the whole list, so the latest event is the current list.
 */
public sealed interface Product extends DomainEvent {

   String productId();

   @Override
   default String aggregateId() {
      return productId();
   }

   @Builder
   record Created(String productId, String sellerId, String title, String description, BigDecimal defaultPrice, List<String> images, long version) implements Product {
   }

   @Builder
   record Updated(String productId, String title, String description, List<String> images, long version) implements Product {
   }

   @Builder
   record Deleted(String productId, long version) implements Product {
   }
}
