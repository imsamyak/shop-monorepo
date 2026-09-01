package com.ecom.inventory.adapter.in.web.controller.reservation.dto.response;

import com.ecom.inventory.port.in.usecase.reservation.dto.result.ExpireReservationsResult;

import java.util.List;
import java.util.UUID;

public record ExpireReservationsResponse(
      int expiredCount,
      List<UUID> expiredReservationIds
) {
   public static ExpireReservationsResponse from(ExpireReservationsResult result) {
      if (result == null) {
         return null;
      }
      return new ExpireReservationsResponse(result.expiredCount(), result.expiredReservationIds());
   }
}
