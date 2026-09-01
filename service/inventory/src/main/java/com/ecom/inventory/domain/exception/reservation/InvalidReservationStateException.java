package com.ecom.inventory.domain.exception.reservation;

import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class InvalidReservationStateException extends BusinessRuleException {

   private final UUID reservationId;
   private final ReservationStatus currentStatus;
   private final String action;

   public InvalidReservationStateException(UUID reservationId, ReservationStatus currentStatus, String action) {
      super(
         String.format("Cannot %s reservation %s with status %s", action, reservationId, currentStatus),
         Map.of(
            "reservationId", reservationId != null ? reservationId.toString() : "null",
            "currentStatus", currentStatus != null ? currentStatus.name() : "null",
            "action", action != null ? action : "null"
         )
      );
      this.reservationId = reservationId;
      this.currentStatus = currentStatus;
      this.action = action;
   }

   public static InvalidReservationStateException of(UUID reservationId, ReservationStatus currentStatus, String action) {
      return new InvalidReservationStateException(reservationId, currentStatus, action);
   }
}
