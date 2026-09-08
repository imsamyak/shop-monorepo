package com.ecom.inventory.port.in.usecase.reservation.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

public record ReservationListResult(List<ReservationResult> reservations) implements Result.Many<ReservationResult> {

   public ReservationListResult {
      reservations = reservations == null ? List.of() : List.copyOf(reservations);
   }

   @Override
   public List<ReservationResult> items() {
      return reservations;
   }
}
