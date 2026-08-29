package com.ecom.contract.inventory;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

/**
 * Domain events emitted by the Inventory boundary for stock mutations.
 */
public sealed interface Inventory extends DomainEvent {

   String skuId();

   @Override
   default String aggregateId() {
      return skuId();
   }

   @Builder
   record StockAdded(String skuId, int quantity, int totalStock) implements Inventory {
   }

   @Builder
   record StockAdjusted(String skuId, int quantityChange, int totalStock) implements Inventory {
   }
}
