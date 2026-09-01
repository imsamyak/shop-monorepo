package com.ecom.inventory.port.in.usecase.reservation.dto.result;

import com.ecom.shared.dto.result.Result;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ExpireReservationsResult(
      int expiredCount,
      List<UUID> expiredReservationIds,
      List<ReservationResult> expired
) implements Result.One {

   public ExpireReservationsResult {
      expiredReservationIds = expiredReservationIds == null ? List.of() : List.copyOf(expiredReservationIds);
      expired = expired == null ? List.of() : List.copyOf(expired);
   }
}
