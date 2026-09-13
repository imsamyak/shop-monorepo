package com.ecom.inventory.port.in.usecase.reservation;

import com.ecom.inventory.port.in.usecase.reservation.dto.query.ListReservationsQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationListResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListReservationsUseCase extends QueryUseCase<ListReservationsQuery, ReservationListResult> {
}
