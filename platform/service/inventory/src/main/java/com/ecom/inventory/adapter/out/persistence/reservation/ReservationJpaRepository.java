package com.ecom.inventory.adapter.out.persistence.reservation;

import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.domain.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReservationJpaRepository extends Repository<InventoryReservation, UUID> {

   Optional<InventoryReservation> findById(UUID id);

   List<InventoryReservation> findByReferenceId(String referenceId);

   List<InventoryReservation> findByReferenceIdAndStatus(String referenceId, ReservationStatus status);

   List<InventoryReservation> findBySkuIdAndStatus(Long skuId, ReservationStatus status);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT r FROM InventoryReservation r WHERE r.id = :id")
   Optional<InventoryReservation> findByIdForUpdate(@Param("id") UUID id);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT r FROM InventoryReservation r WHERE r.id IN :ids ORDER BY r.id")
   List<InventoryReservation> findByIdsForUpdate(@Param("ids") Collection<UUID> ids);

   @Query("SELECT r.id FROM InventoryReservation r WHERE r.status = :status AND r.expiresAt < :now ORDER BY r.expiresAt")
   List<UUID> findIdsByStatusAndExpiresAtBefore(@Param("status") ReservationStatus status,
                                                 @Param("now") Instant now,
                                                 Pageable pageable);

   InventoryReservation saveAndFlush(InventoryReservation reservation);

   <S extends InventoryReservation> List<S> saveAllAndFlush(Iterable<S> entities);
}
