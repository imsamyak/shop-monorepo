package com.ecom.inventory.port.in.usecase.reservation.dto.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Releases several reservations atomically. Already released or expired reservations are accepted, so cancelling is
 * idempotent; a committed reservation cannot be released.
 */
@Builder
public record ReleaseReservationsCmd(
      @NotEmpty @Size(max = 100) List<@NotNull UUID> reservationIds
) {
}
