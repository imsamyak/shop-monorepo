package com.ecom.contract.inventory;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.util.List;

/**
 * Domain events emitted by the Inventory boundary for reservation lifecycle mutations. {@code version} is the
 * reservation's version after the change (see {@code Product}).
 */
public sealed interface Reservation extends DomainEvent {

   String reservationId();

   @Override
   default String aggregateId() {
      return reservationId();
   }

   @Builder
   record Created(String reservationId, String checkoutId, List<ReservedItem> items, long version) implements Reservation {
   }

   @Builder
   record Released(String reservationId, String reason, long version) implements Reservation {
   }

   @Builder
   record Committed(String reservationId, String checkoutId, long version) implements Reservation {
   }

   @Builder
   record Expired(String reservationId, String checkoutId, long version) implements Reservation {
   }

   @Builder
   record ReservedItem(String skuId, int quantity) {
   }
}
