package com.ecom.inventory.port.in.usecase.reservation.dto.result;

import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.shared.dto.result.Result;

import java.util.List;
import java.util.UUID;

/**
 * @param reservations every reservation the request was about
 * @param changedIds   the ones whose state this call actually changed. Empty when the call was an idempotent replay,
 *                     in which case no events are emitted.
 * @param stock        the stock of every item this call changed, after the change; each becomes an availability event
 */
public record ReservationsResult(
      List<ReservationResult> reservations,
      List<UUID> changedIds,
      List<InventoryResult> stock
) implements Result.Many<ReservationResult> {

   public ReservationsResult {
      reservations = reservations == null ? List.of() : List.copyOf(reservations);
      changedIds = changedIds == null ? List.of() : List.copyOf(changedIds);
      stock = stock == null ? List.of() : List.copyOf(stock);
   }

   @Override
   public List<ReservationResult> items() {
      return reservations;
   }

   public boolean isReplay() {
      return changedIds.isEmpty();
   }
}
