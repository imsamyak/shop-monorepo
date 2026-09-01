package com.ecom.inventory.adapter.out.persistence.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationForUpdatePort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.port.out.persistence.reservation.SaveReservationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationPersistenceAdapter implements
      LoadReservationPort,
      LoadReservationForUpdatePort,
      SaveReservationPort {

   private final ReservationJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<InventoryReservation> loadById(UUID reservationId) {
      return repository.findById(reservationId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<InventoryReservation> findByReferenceId(String referenceId) {
      return repository.findByReferenceId(referenceId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<InventoryReservation> findPendingBySkuId(Long skuId) {
      return repository.findBySkuIdAndStatus(skuId, ReservationStatus.PENDING);
   }

   @Override
   @Transactional(readOnly = true)
   public List<UUID> findExpiredPendingIds(Instant now, int limit) {
      return repository.findIdsByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now, PageRequest.of(0, limit));
   }

   @Override
   @Transactional
   public Optional<InventoryReservation> loadByIdForUpdate(UUID reservationId) {
      return repository.findByIdForUpdate(reservationId);
   }

   @Override
   @Transactional
   public List<InventoryReservation> loadByIdsForUpdate(Collection<UUID> reservationIds) {
      if (reservationIds == null || reservationIds.isEmpty()) {
         return List.of();
      }
      return repository.findByIdsForUpdate(reservationIds);
   }

   @Override
   @Transactional
   public InventoryReservation save(InventoryReservation reservation) {
      return repository.saveAndFlush(reservation);
   }

   @Override
   @Transactional
   public List<InventoryReservation> saveAll(List<InventoryReservation> reservations) {
      return repository.saveAllAndFlush(reservations);
   }
}
