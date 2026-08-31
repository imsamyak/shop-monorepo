package com.ecom.listing.adapter.out.persistence.product;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.port.out.persistence.product.DeleteProductPort;
import com.ecom.listing.port.out.persistence.product.LoadProductForUpdatePort;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.port.out.persistence.product.SaveProductPort;
import com.ecom.listing.port.out.persistence.product.UpdateProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ProductPersistenceAdapter
      implements LoadProductPort, LoadProductForUpdatePort, SaveProductPort, UpdateProductPort, DeleteProductPort {

   private final ProductRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Product> load(UUID id) {
      return repository.findById(id);
   }

   @Override
   @Transactional(readOnly = true)
   public List<Product> load(Iterable<UUID> ids) {
      return repository.findAllById(ids);
   }

   @Override
   @Transactional
   public Optional<Product> loadForUpdate(UUID id) {
      return repository.findByIdForUpdate(id);
   }

   /** Flushes immediately so constraint violations surface inside the use case, not at commit time. */
   @Override
   @Transactional
   public Product save(Product product) {
      return repository.saveAndFlush(product);
   }
}
