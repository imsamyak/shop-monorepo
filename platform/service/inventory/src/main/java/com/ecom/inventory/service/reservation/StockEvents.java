package com.ecom.inventory.service.reservation;

import com.ecom.contract.inventory.Inventory;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.shared.messaging.DomainEvent;

import java.util.List;

/**
 * The events that tell other services how a reservation changed the stock of the items it touched.
 */
final class StockEvents {

   private StockEvents() {
   }

   static List<DomainEvent> availabilityChanged(List<InventoryResult> stock) {
      return stock.stream()
            .map(item -> (DomainEvent) new Inventory.AvailabilityChanged(String.valueOf(item.skuId()),
                  item.totalQuantity(), item.reservedQuantity(), item.availableQuantity(), item.version()))
            .toList();
   }
}
