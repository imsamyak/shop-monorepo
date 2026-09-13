package com.ecom.inventory.service.reservation;

import com.ecom.contract.inventory.Reservation;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationStateException;
import com.ecom.inventory.domain.exception.reservation.ReservationExpiredException;
import com.ecom.inventory.domain.exception.reservation.ReservationNotFoundException;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.in.usecase.reservation.CommitReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.CommitReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationForUpdatePort;
import com.ecom.inventory.port.out.persistence.reservation.SaveReservationPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import com.ecom.inventory.service.reservation.mapper.ReservationMapper;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Commits reservations all-or-nothing. Already committed reservations are skipped, which makes the call idempotent
 * (a retry after the caller failed to record the result is safe). Any reservation that is expired, released or
 * unknown fails the whole call before anything is changed.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CommitReservationService implements CommitReservationUseCase {

   private final LoadReservationForUpdatePort loadReservationForUpdatePort;
   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final SaveReservationPort saveReservationPort;
   private final ReservationMapper reservationMapper;
   private final InventoryMapper inventoryMapper;

   @Override
   @Transactional
   public ReservationsResult execute(CommitReservationsCmd cmd) {
      Set<UUID> ids = Set.copyOf(cmd.reservationIds());
      List<InventoryReservation> reservations = loadReservationForUpdatePort.loadByIdsForUpdate(ids);

      Set<UUID> found = reservations.stream().map(InventoryReservation::getId).collect(Collectors.toSet());
      for (UUID id : ids) {
         if (!found.contains(id)) {
            throw ReservationNotFoundException.of(id);
         }
      }

      Instant now = Instant.now();
      List<InventoryReservation> toCommit = reservations.stream()
            .filter(r -> r.getStatus() != ReservationStatus.COMMITTED)
            .toList();
      for (InventoryReservation reservation : toCommit) {
         if (!reservation.isPending()) {
            throw InvalidReservationStateException.of(reservation.getId(), reservation.getStatus(), "commit");
         }
         if (reservation.isExpired(now)) {
            throw ReservationExpiredException.of(reservation.getId(), reservation.getExpiresAt());
         }
      }

      Map<Long, InventoryResult> stock = new LinkedHashMap<>();
      if (!toCommit.isEmpty()) {
         Map<Long, InventoryItem> items = loadInventoryForUpdatePort
               .loadBySkuIdsForUpdate(toCommit.stream().map(InventoryReservation::getSkuId).collect(Collectors.toSet()))
               .stream()
               .collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));

         for (InventoryReservation reservation : toCommit) {
            InventoryItem item = items.get(reservation.getSkuId());
            if (item == null) {
               throw InventoryNotFoundException.of(reservation.getSkuId());
            }
            item.commit(reservation.getQuantity());
            reservation.commit(now);
            stock.put(item.getSkuId(), inventoryMapper.toResult(saveInventoryPort.save(item), 0));
         }
         saveReservationPort.saveAll(toCommit);
      }

      return new ReservationsResult(
            reservationMapper.toResultList(reservations),
            toCommit.stream().map(InventoryReservation::getId).toList(),
            new ArrayList<>(stock.values()));
   }

   @Override
   public List<DomainEvent> buildEvents(CommitReservationsCmd cmd, ReservationsResult result) {
      if (result == null) {
         return List.of();
      }
      Set<UUID> changed = Set.copyOf(result.changedIds());
      List<DomainEvent> events = new ArrayList<>(result.reservations().stream()
            .filter(r -> changed.contains(r.reservationId()))
            .map(r -> (DomainEvent) new Reservation.Committed(r.reservationId().toString(), r.referenceId(), r.version()))
            .toList());
      events.addAll(StockEvents.availabilityChanged(result.stock()));
      return events;
   }
}
