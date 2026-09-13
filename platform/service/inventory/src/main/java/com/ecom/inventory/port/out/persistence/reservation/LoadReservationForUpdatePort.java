package com.ecom.inventory.port.out.persistence.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadReservationForUpdatePort {

   Optional<InventoryReservation> loadByIdForUpdate(UUID reservationId);

   /** Locks the given reservations in ascending id order. Missing ids are absent from the result. */
   List<InventoryReservation> loadByIdsForUpdate(Collection<UUID> reservationIds);
}
