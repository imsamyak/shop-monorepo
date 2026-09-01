package com.ecom.inventory.service.reservation;

import com.ecom.contract.inventory.Reservation;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.port.in.usecase.reservation.ExpireReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ExpireReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ExpireReservationsResult;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationForUpdatePort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.port.out.persistence.reservation.SaveReservationPort;
import com.ecom.inventory.service.reservation.mapper.ReservationMapper;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Expires overdue pending reservations and returns their stock. Each reservation is locked (and re-checked) before
 * it is touched, so a concurrent commit or release of the same reservation cannot be overwritten. Lock order is
 * reservation first, then inventory item, the same as commit and release.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ExpireReservationsService implements ExpireReservationsUseCase {

   private final LoadReservationPort loadReservationPort;
   private final LoadReservationForUpdatePort loadReservationForUpdatePort;
   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final SaveReservationPort saveReservationPort;
   private final ReservationMapper reservationMapper;

   @Override
   @Transactional
   public ExpireReservationsResult execute(ExpireReservationsCmd cmd) {
      Instant now = (cmd != null && cmd.referenceTime() != null) ? cmd.referenceTime() : Instant.now();
      int limit = cmd != null ? cmd.effectiveLimit() : ExpireReservationsCmd.DEFAULT_LIMIT;

      List<UUID> candidates = loadReservationPort.findExpiredPendingIds(now, limit);

      List<ReservationResult> expired = new ArrayList<>();
      for (UUID id : candidates) {
         Optional<InventoryReservation> locked = loadReservationForUpdatePort.loadByIdForUpdate(id);
         if (locked.isEmpty()) {
            continue;
         }
         InventoryReservation reservation = locked.get();
         if (!reservation.isPending() || !reservation.isExpired(now)) {
            continue;
         }

         reservation.expire();
         Optional<InventoryItem> item = loadInventoryForUpdatePort.loadBySkuIdForUpdate(reservation.getSkuId());
         if (item.isPresent()) {
            item.get().release(reservation.getQuantity());
            saveInventoryPort.save(item.get());
         }
         saveReservationPort.save(reservation);
         expired.add(reservationMapper.toResult(reservation));
      }

      return new ExpireReservationsResult(expired.size(),
            expired.stream().map(ReservationResult::reservationId).toList(), expired);
   }

   @Override
   public List<DomainEvent> buildEvents(ExpireReservationsCmd cmd, ExpireReservationsResult result) {
      if (result == null) {
         return List.of();
      }
      return result.expired().stream()
            .map(r -> (DomainEvent) new Reservation.Expired(r.reservationId().toString(), r.referenceId()))
            .toList();
   }
}
