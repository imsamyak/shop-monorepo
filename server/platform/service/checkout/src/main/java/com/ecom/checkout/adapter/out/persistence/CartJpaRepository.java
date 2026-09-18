package com.ecom.checkout.adapter.out.persistence;

import com.ecom.checkout.domain.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface CartJpaRepository extends Repository<Cart, UUID> {

   Optional<Cart> findByCustomerId(UUID customerId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT c FROM Cart c WHERE c.customerId = :customerId")
   Optional<Cart> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);

   Cart saveAndFlush(Cart cart);
}
