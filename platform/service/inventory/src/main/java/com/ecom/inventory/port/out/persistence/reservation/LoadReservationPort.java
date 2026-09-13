package com.ecom.inventory.port.out.persistence.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadReservationPort {

   Optional<InventoryReservation> loadById(UUID reservationId);

   List<InventoryReservation> findByReferenceId(String referenceId);

   List<InventoryReservation> findByReferenceIdAndStatus(String referenceId, ReservationStatus status);

   List<InventoryReservation> findPendingBySkuId(Long skuId);

   /** Ids of pending reservations that expired before {@code now}, oldest first, at most {@code limit}. */
   List<UUID> findExpiredPendingIds(Instant now, int limit);
}
