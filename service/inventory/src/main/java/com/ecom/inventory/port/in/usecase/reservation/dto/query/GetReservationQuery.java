package com.ecom.inventory.port.in.usecase.reservation.dto.query;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetReservationQuery(@NotNull UUID reservationId) {
}
