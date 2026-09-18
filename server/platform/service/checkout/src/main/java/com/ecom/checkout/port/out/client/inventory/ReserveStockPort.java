package com.ecom.checkout.port.out.client.inventory;

import java.util.List;
import java.util.UUID;

public interface ReserveStockPort {

   /**
     * Reserves every line atomically (all or nothing). Repeating the call with the same {@code referenceId} returns
     * the original reservations.
     */
   List<ReservedStock> reserve(String referenceId, long ttlSeconds, List<StockLine> lines);

   record StockLine(Long skuId, int quantity) {
   }

   record ReservedStock(Long skuId, UUID reservationId) {
   }
}
