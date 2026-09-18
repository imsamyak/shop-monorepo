package com.ecom.inventory.port.in.usecase.reservation.dto.query;

import com.ecom.inventory.domain.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The reservations held for one checkout (the reference id), optionally only those in one status.
 */
public record ListReservationsQuery(
      @NotBlank @Size(max = 100) String checkoutId,
      ReservationStatus status
) {
}
