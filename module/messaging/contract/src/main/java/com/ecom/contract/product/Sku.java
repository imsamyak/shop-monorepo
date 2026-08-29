package com.ecom.contract.product;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Domain events emitted by the Product boundary for Sku lifecycle mutations.
 */
public sealed interface Sku extends DomainEvent {

   String skuId();

   @Override
   default String aggregateId() {
      return skuId();
   }

   @Builder
   record Created(String skuId, String productId, String sellerId, BigDecimal price) implements Sku {
   }

   @Builder
   record Updated(String skuId, String productId, BigDecimal price) implements Sku {
   }

   @Builder
   record Deleted(String skuId, String productId) implements Sku {
   }
}
