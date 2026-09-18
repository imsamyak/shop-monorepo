package com.ecom.contract.inventory;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

/**
 * Domain events emitted by the Inventory boundary for stock mutations.
 * <p>
 * Every event is a snapshot of the SKU's stock after the change: {@code version} is the inventory item's version, which
 * only grows, so a consumer that keeps the snapshot with the highest version always has the current stock.
 * {@code availableStock} is the total minus what is reserved.
 */
public sealed interface Inventory extends DomainEvent {

   String skuId();

   @Override
   default String aggregateId() {
      return skuId();
   }

   @Builder
   record StockAdded(String skuId, int quantity, int totalStock, int availableStock, long version) implements Inventory {
   }

   @Builder
   record StockAdjusted(String skuId, int quantityChange, int totalStock, int availableStock, long version) implements Inventory {
   }

   /**
    * The reserved or total stock changed because a reservation was made, released, expired or committed.
    */
   @Builder
   record AvailabilityChanged(String skuId, int totalStock, int reservedStock, int availableStock, long version) implements Inventory {
   }
}
