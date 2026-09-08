package com.ecom.inventory.service.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.port.in.usecase.reservation.ListReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.query.ListReservationsQuery;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationListResult;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.service.reservation.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class ListReservationsService implements ListReservationsUseCase {

   private final LoadReservationPort loadReservationPort;
   private final ReservationMapper reservationMapper;

   @Override
   public ReservationListResult execute(ListReservationsQuery query) {
      List<InventoryReservation> reservations = query.status() == null
            ? loadReservationPort.findByReferenceId(query.checkoutId())
            : loadReservationPort.findByReferenceIdAndStatus(query.checkoutId(), query.status());
      return new ReservationListResult(reservationMapper.toResultList(
            reservations.stream().sorted(Comparator.comparing(InventoryReservation::getSkuId)).toList()));
   }
}
