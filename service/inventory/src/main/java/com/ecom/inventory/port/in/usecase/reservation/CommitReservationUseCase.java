package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.CommitReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;
import com.ecom.outbox.OutboxUseCase;

public interface CommitReservationUseCase extends OutboxUseCase<CommitReservationsCmd, ReservationsResult> {
}
