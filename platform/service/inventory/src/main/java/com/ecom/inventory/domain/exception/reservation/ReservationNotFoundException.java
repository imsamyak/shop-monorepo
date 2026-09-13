package com.ecom.inventory.domain.exception.reservation;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ReservationNotFoundException extends EntityNotFoundException {

   private final UUID reservationId;

   public ReservationNotFoundException(UUID reservationId) {
      super("InventoryReservation", reservationId);
      this.reservationId = reservationId;
   }

   public static ReservationNotFoundException of(UUID reservationId) {
      return new ReservationNotFoundException(reservationId);
   }
}
