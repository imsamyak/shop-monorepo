package com.ecom.inventory.service.reservation;

import com.ecom.contract.inventory.Reservation;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.port.in.usecase.reservation.ReserveInventoryUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ReserveInventoryCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ReservationsResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserves all requested lines or none (one transaction). Repeating a request with the same {@code referenceId}
 * returns the original reservations, so a client that never saw the response can safely retry.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ReserveInventoryService implements ReserveInventoryUseCase {

   private final LoadReservationPort loadReservationPort;
   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final SaveReservationPort saveReservationPort;
   private final ReservationMapper reservationMapper;

   @Override
   @Transactional
   public ReservationsResult execute(ReserveInventoryCmd cmd) {
      List<InventoryReservation> existing = loadReservationPort.findByReferenceId(cmd.referenceId());
      if (!existing.isEmpty()) {
         return new ReservationsResult(reservationMapper.toResultList(existing), List.of());
      }

      Map<Long, Integer> wanted = new TreeMap<>();
      for (ReserveInventoryCmd.Item item : cmd.items()) {
         wanted.merge(item.skuId(), item.quantity(), Integer::sum);
      }

      Map<Long, InventoryItem> items = loadInventoryForUpdatePort.loadBySkuIdsForUpdate(wanted.keySet()).stream()
            .collect(Collectors.toMap(InventoryItem::getSkuId, Function.identity()));

      Instant expiresAt = Instant.now().plusSeconds(cmd.ttlSeconds());
      List<InventoryReservation> reservations = new ArrayList<>();
      for (Map.Entry<Long, Integer> line : wanted.entrySet()) {
         InventoryItem item = items.get(line.getKey());
         if (item == null) {
            throw InventoryNotFoundException.of(line.getKey());
         }
         item.reserve(line.getValue());
         saveInventoryPort.save(item);
         reservations.add(InventoryReservation.of(cmd.referenceId(), line.getKey(), line.getValue(), expiresAt));
      }

      List<InventoryReservation> saved = saveReservationPort.saveAll(reservations);
      return new ReservationsResult(
            reservationMapper.toResultList(saved),
            saved.stream().map(InventoryReservation::getId).toList());
   }

   @Override
   public List<DomainEvent> buildEvents(ReserveInventoryCmd cmd, ReservationsResult result) {
      if (result == null || result.isReplay()) {
         return List.of();
      }
      return result.reservations().stream()
            .map(r -> (DomainEvent) new Reservation.Created(
                  r.reservationId().toString(),
                  r.referenceId(),
                  List.of(new Reservation.ReservedItem(String.valueOf(r.skuId()), r.quantity()))))
            .toList();
   }
}
