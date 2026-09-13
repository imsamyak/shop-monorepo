package com.ecom.order.adapter.out.persistence;

import com.ecom.order.domain.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends Repository<Order, UUID>, JpaSpecificationExecutor<Order> {

   Optional<Order> findById(UUID id);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT o FROM CustomerOrder o WHERE o.id = :id")
   Optional<Order> findByIdForUpdate(@Param("id") UUID id);

   Order saveAndFlush(Order order);
}
