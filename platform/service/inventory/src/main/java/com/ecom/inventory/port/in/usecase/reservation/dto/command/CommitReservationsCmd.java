package com.ecom.inventory.port.in.usecase.reservation.dto.command;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Commits several reservations atomically (all or none). Already-committed reservations are accepted, so a retry
 * after a partial failure elsewhere is safe.
 */
@Builder
public record CommitReservationsCmd(
      @NotEmpty @Size(max = 100) List<@NotNull UUID> reservationIds
) {
}
