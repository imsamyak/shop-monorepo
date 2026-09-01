package com.ecom.inventory.port.in.usecase.reservation.dto.result;

import com.ecom.inventory.domain.entity.ReservationStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ReservationResult(
      UUID reservationId,
      String referenceId,
      Long skuId,
      int quantity,
      ReservationStatus status,
      Instant expiresAt
) {
}
