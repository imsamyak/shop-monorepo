package com.ecom.inventory.domain.exception.reservation;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
public class ReservationExpiredException extends BusinessRuleException {

   private final UUID reservationId;
   private final Instant expiresAt;

   public ReservationExpiredException(UUID reservationId, Instant expiresAt) {
      super(
         String.format("Reservation %s expired at %s", reservationId, expiresAt),
         Map.of(
            "reservationId", reservationId != null ? reservationId.toString() : "null",
            "expiresAt", expiresAt != null ? expiresAt.toString() : "null"
         )
      );
      this.reservationId = reservationId;
      this.expiresAt = expiresAt;
   }

   public static ReservationExpiredException of(UUID reservationId, Instant expiresAt) {
      return new ReservationExpiredException(reservationId, expiresAt);
   }
}
