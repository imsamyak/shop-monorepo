package com.ecom.listing.adapter.out.persistence.product;

import com.ecom.listing.domain.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductRepository extends Repository<Product, UUID>, JpaSpecificationExecutor<Product> {

   Optional<Product> findById(UUID id);

   List<Product> findAllById(Iterable<UUID> ids);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT p FROM Product p WHERE p.id = :id")
   Optional<Product> findByIdForUpdate(@Param("id") UUID id);

   Product saveAndFlush(Product product);
}
