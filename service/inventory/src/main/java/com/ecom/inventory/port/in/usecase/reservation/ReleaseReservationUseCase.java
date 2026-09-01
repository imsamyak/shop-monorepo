package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReleaseReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;
import com.ecom.outbox.OutboxUseCase;

public interface ReleaseReservationUseCase extends OutboxUseCase<ReleaseReservationsCmd, ReservationsResult> {
}
