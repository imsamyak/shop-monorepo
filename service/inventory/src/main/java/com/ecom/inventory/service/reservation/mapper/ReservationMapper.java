package com.ecom.inventory.service.reservation.mapper;

import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class ReservationMapper {

   public ReservationResult toResult(InventoryReservation reservation) {
      if (reservation == null) {
         return null;
      }
      return ReservationResult.builder()
            .reservationId(reservation.getId())
            .referenceId(reservation.getReferenceId())
            .skuId(reservation.getSkuId())
            .quantity(reservation.getQuantity())
            .status(reservation.getStatus())
            .expiresAt(reservation.getExpiresAt())
            .version(reservation.version())
            .build();
   }

   public List<ReservationResult> toResultList(Collection<InventoryReservation> reservations) {
      if (reservations == null) {
         return List.of();
      }
      return reservations.stream().map(this::toResult).toList();
   }
}
