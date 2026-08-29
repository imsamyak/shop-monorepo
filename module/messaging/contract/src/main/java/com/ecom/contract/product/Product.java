package com.ecom.contract.product;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Domain events emitted by the Product boundary for Product lifecycle mutations.
 */
public sealed interface Product extends DomainEvent {

   String productId();

   @Override
   default String aggregateId() {
      return productId();
   }

   @Builder
   record Created(String productId, String sellerId, String title, String description, BigDecimal defaultPrice) implements Product {
   }

   @Builder
   record Updated(String productId, String title, String description) implements Product {
   }

   @Builder
   record Deleted(String productId) implements Product {
   }
}
