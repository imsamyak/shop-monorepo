package com.ecom.inventory.adapter.in.web.controller.reservation.dto.response;

import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;

import java.util.List;

public record ReservationsResponse(List<ReservationResponse> reservations) {

   public static ReservationsResponse from(ReservationsResult result) {
      return new ReservationsResponse(result.reservations().stream().map(ReservationResponse::from).toList());
   }
}
