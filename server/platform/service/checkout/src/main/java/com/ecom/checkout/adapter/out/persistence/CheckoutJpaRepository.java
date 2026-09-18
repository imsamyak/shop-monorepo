package com.ecom.checkout.adapter.out.persistence;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CheckoutJpaRepository extends Repository<Checkout, UUID> {

   Optional<Checkout> findById(UUID id);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT c FROM Checkout c WHERE c.id = :id")
   Optional<Checkout> findByIdForUpdate(@Param("id") UUID id);

   @Query("SELECT c.id FROM Checkout c WHERE c.status = :status AND c.expiresAt < :now ORDER BY c.expiresAt")
   List<UUID> findIdsByStatusAndExpiresAtBefore(@Param("status") CheckoutStatus status,
                                                 @Param("now") Instant now,
                                                 Pageable pageable);

   Checkout saveAndFlush(Checkout checkout);
}
