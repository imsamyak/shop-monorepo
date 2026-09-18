package com.ecom.inventory.adapter.in.web.controller.reservation.dto.response;

import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationListResult;

import java.util.List;

public record ReservationListResponse(List<ReservationResponse> reservations) {

   public static ReservationListResponse from(ReservationListResult result) {
      return new ReservationListResponse(result.reservations().stream().map(ReservationResponse::from).toList());
   }
}
