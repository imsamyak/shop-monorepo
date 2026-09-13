package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.ExpireReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ExpireReservationsResult;
import com.ecom.outbox.OutboxUseCase;

public interface ExpireReservationsUseCase extends OutboxUseCase<ExpireReservationsCmd, ExpireReservationsResult> {
}
