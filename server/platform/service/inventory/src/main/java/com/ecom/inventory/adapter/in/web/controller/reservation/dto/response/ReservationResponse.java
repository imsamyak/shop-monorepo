package com.ecom.inventory.adapter.in.web.controller.reservation.dto.response;

import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
      UUID reservationId,
      String referenceId,
      Long skuId,
      int quantity,
      ReservationStatus status,
      Instant expiresAt
) {
   public static ReservationResponse from(ReservationResult result) {
      return new ReservationResponse(
            result.reservationId(),
            result.referenceId(),
            result.skuId(),
            result.quantity(),
            result.status(),
            result.expiresAt()
      );
   }
}
