package com.ecom.inventory.adapter.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.port.out.persistence.item.DeleteInventoryPort;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements
      LoadInventoryPort,
      LoadInventoryForUpdatePort,
      SaveInventoryPort,
      DeleteInventoryPort {

   private final InventoryJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<InventoryItem> loadBySkuId(Long skuId) {
      return repository.findBySkuId(skuId);
   }

   @Override
   @Transactional
   public Optional<InventoryItem> loadBySkuIdForUpdate(Long skuId) {
      return repository.findBySkuIdForUpdate(skuId);
   }

   @Override
   @Transactional
   public List<InventoryItem> loadBySkuIdsForUpdate(Collection<Long> skuIds) {
      if (skuIds == null || skuIds.isEmpty()) {
         return List.of();
      }
      return repository.findBySkuIdsForUpdate(skuIds);
   }

   @Override
   @Transactional
   public InventoryItem save(InventoryItem inventoryItem) {
      return repository.saveAndFlush(inventoryItem);
   }

   @Override
   @Transactional
   public void deleteBySkuId(Long skuId) {
      repository.deleteBySkuId(skuId);
   }
}
