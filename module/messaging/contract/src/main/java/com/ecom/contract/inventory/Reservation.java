package com.ecom.contract.inventory;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.util.List;

/**
 * Domain events emitted by the Inventory boundary for reservation lifecycle mutations.
 */
public sealed interface Reservation extends DomainEvent {

   String reservationId();

   @Override
   default String aggregateId() {
      return reservationId();
   }

   @Builder
   record Created(String reservationId, String checkoutId, List<ReservedItem> items) implements Reservation {
   }

   @Builder
   record Released(String reservationId, String reason) implements Reservation {
   }

   @Builder
   record Committed(String reservationId, String checkoutId) implements Reservation {
   }

   @Builder
   record Expired(String reservationId, String checkoutId) implements Reservation {
   }

   @Builder
   record ReservedItem(String skuId, int quantity) {
   }
}
