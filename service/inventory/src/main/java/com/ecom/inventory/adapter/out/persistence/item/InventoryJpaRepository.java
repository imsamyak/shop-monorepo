package com.ecom.inventory.adapter.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface InventoryJpaRepository extends Repository<InventoryItem, Long> {

   Optional<InventoryItem> findBySkuId(Long skuId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT i FROM InventoryItem i WHERE i.skuId = :skuId")
   Optional<InventoryItem> findBySkuIdForUpdate(@Param("skuId") Long skuId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT i FROM InventoryItem i WHERE i.skuId IN :skuIds ORDER BY i.skuId")
   List<InventoryItem> findBySkuIdsForUpdate(@Param("skuIds") Collection<Long> skuIds);

   InventoryItem saveAndFlush(InventoryItem item);

   @Modifying
   @Query("DELETE FROM InventoryItem i WHERE i.skuId = :skuId")
   void deleteBySkuId(@Param("skuId") Long skuId);
}
