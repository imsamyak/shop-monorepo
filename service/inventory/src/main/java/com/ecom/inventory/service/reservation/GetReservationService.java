package com.ecom.inventory.service.reservation;

import com.ecom.inventory.domain.exception.reservation.ReservationNotFoundException;
import com.ecom.inventory.port.in.usecase.reservation.GetReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.GetReservationQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.service.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetReservationService implements GetReservationUseCase {

   private final LoadReservationPort loadReservationPort;
   private final ReservationMapper reservationMapper;

   @Override
   public ReservationResult execute(GetReservationQuery query) {
      return loadReservationPort.loadById(query.reservationId())
            .map(reservationMapper::toResult)
            .orElseThrow(() -> ReservationNotFoundException.of(query.reservationId()));
   }
}
