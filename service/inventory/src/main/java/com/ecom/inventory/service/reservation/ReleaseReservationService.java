package com.ecom.inventory.service.reservation;

import com.ecom.contract.inventory.Reservation;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationStateException;
import com.ecom.inventory.domain.exception.reservation.ReservationNotFoundException;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.in.usecase.reservation.ReleaseReservationUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReleaseReservationsCmd;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Releases reservations all-or-nothing and idempotently: reservations that are already released or expired are left
 * alone, a committed reservation cannot be released.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ReleaseReservationService implements ReleaseReservationUseCase {

   private final LoadReservationForUpdatePort loadReservationForUpdatePort;
   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final SaveReservationPort saveReservationPort;
   private final ReservationMapper reservationMapper;
   private final InventoryMapper inventoryMapper;

   @Override
   @Transactional
   public ReservationsResult execute(ReleaseReservationsCmd cmd) {
      Set<UUID> ids = Set.copyOf(cmd.reservationIds());
      List<InventoryReservation> reservations = loadReservationForUpdatePort.loadByIdsForUpdate(ids);

      Set<UUID> found = reservations.stream().map(InventoryReservation::getId).collect(Collectors.toSet());
      for (UUID id : ids) {
         if (!found.contains(id)) {
            throw ReservationNotFoundException.of(id);
         }
      }

      for (InventoryReservation reservation : reservations) {
         if (reservation.getStatus() == ReservationStatus.COMMITTED) {
            throw InvalidReservationStateException.of(reservation.getId(), reservation.getStatus(), "release");
         }
      }

      List<InventoryReservation> toRelease = reservations.stream().filter(InventoryReservation::isPending).toList();
      Map<Long, InventoryResult> stock = new LinkedHashMap<>();
      if (!toRelease.isEmpty()) {
         Map<Long, InventoryItem> items = loadInventoryForUpdatePort
               .loadBySkuIdsForUpdate(toRelease.stream().map(InventoryReservation::getSkuId).collect(Collectors.toSet()))
               .stream()
               .collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));

         for (InventoryReservation reservation : toRelease) {
            InventoryItem item = items.get(reservation.getSkuId());
            if (item == null) {
               throw InventoryNotFoundException.of(reservation.getSkuId());
            }
            item.release(reservation.getQuantity());
            reservation.release();
            stock.put(item.getSkuId(), inventoryMapper.toResult(saveInventoryPort.save(item), 0));
         }
         saveReservationPort.saveAll(toRelease);
      }

      return new ReservationsResult(
            reservationMapper.toResultList(reservations),
            toRelease.stream().map(InventoryReservation::getId).toList(),
            new ArrayList<>(stock.values()));
   }

   @Override
   public List<DomainEvent> buildEvents(ReleaseReservationsCmd cmd, ReservationsResult result) {
      if (result == null) {
         return List.of();
      }
      Set<UUID> changed = Set.copyOf(result.changedIds());
      List<DomainEvent> events = new ArrayList<>(result.reservations().stream()
            .filter(r -> changed.contains(r.reservationId()))
            .map(r -> (DomainEvent) new Reservation.Released(r.reservationId().toString(), "RELEASED", r.version()))
            .toList());
      events.addAll(StockEvents.availabilityChanged(result.stock()));
      return events;
   }
}
