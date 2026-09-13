package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReserveInventoryCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;
import com.ecom.outbox.OutboxUseCase;

public interface ReserveInventoryUseCase extends OutboxUseCase<ReserveInventoryCmd, ReservationsResult> {
}
