package com.ecom.listing.adapter.out.persistence.sku;

import com.ecom.listing.domain.converter.SkuPropertyMapConverter;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.port.out.persistence.sku.DeleteSkuPort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuForUpdatePort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.port.out.persistence.sku.SaveSkuPort;
import com.ecom.listing.port.out.persistence.sku.UpdateSkuPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class SkuPersistenceAdapter
      implements LoadSkuPort, LoadSkuForUpdatePort, SaveSkuPort, UpdateSkuPort, DeleteSkuPort {

   private final SkuRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Sku> load(UUID productId, Long skuId) {
      return repository.findByIdAndProductId(skuId, productId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Sku> load(UUID productId, Collection<Long> skuIds) {
      if (skuIds == null || skuIds.isEmpty()) {
         return List.of();
      }
      return repository.findByProductIdAndIdIn(productId, skuIds);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Sku> loadAll(Collection<Long> skuIds) {
      if (skuIds == null || skuIds.isEmpty()) {
         return List.of();
      }
      return repository.findAllByIdIn(skuIds);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Sku> loadByProduct(UUID productId) {
      return repository.findByProductId(productId);
   }

   @Override
   @Transactional(readOnly = true)
   public boolean existsWithProperties(UUID productId, Map<String, String> properties, Long excludingSkuId) {
      return repository.existsWithProperties(productId, SkuPropertyMapConverter.sorted(properties), excludingSkuId);
   }

   @Override
   @Transactional
   public Optional<Sku> loadForUpdate(UUID productId, Long skuId) {
      return repository.findByIdAndProductIdForUpdate(skuId, productId);
   }

   /** Flushes immediately so constraint violations surface inside the use case, not at commit time. */
   @Override
   @Transactional
   public Sku save(Sku sku) {
      return repository.saveAndFlush(sku);
   }
}
