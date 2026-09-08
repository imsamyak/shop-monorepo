package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.query.GetReservationQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetReservationUseCase extends QueryUseCase<GetReservationQuery, ReservationResult> {
}
