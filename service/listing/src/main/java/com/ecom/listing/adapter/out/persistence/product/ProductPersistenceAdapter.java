package com.ecom.listing.adapter.out.persistence.product;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.port.out.persistence.product.DeleteProductPort;
import com.ecom.listing.port.out.persistence.product.LoadProductForUpdatePort;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.port.in.usecase.product.dto.query.ProductSort;
import com.ecom.listing.port.out.persistence.product.SaveProductPort;
import com.ecom.listing.port.out.persistence.product.SearchProductsPort;
import com.ecom.listing.port.out.persistence.product.UpdateProductPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class ProductPersistenceAdapter
      implements LoadProductPort, LoadProductForUpdatePort, SaveProductPort, UpdateProductPort, DeleteProductPort,
      SearchProductsPort {

   private static final char ESCAPE = '\\';

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

   @Override
   @Transactional(readOnly = true)
   public Found search(UUID sellerId, String title, int page, int size, ProductSort sort) {
      Specification<Product> spec = (root, query, cb) -> cb.conjunction();
      if (sellerId != null) {
         spec = spec.and((root, query, cb) -> cb.equal(root.get("sellerId"), sellerId));
      }
      if (title != null) {
         String pattern = "%" + escapeLike(title.toLowerCase(Locale.ROOT)) + "%";
         spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, ESCAPE));
      }
      Page<Product> found = repository.findAll(spec, PageRequest.of(page, size, orderBy(sort)));
      return new Found(found.getContent(), found.getTotalElements());
   }

   private static Sort orderBy(ProductSort sort) {
      Sort primary = switch (sort) {
         case CREATED_AT_DESC -> Sort.by(Sort.Direction.DESC, "ts.createdAt");
         case CREATED_AT_ASC -> Sort.by(Sort.Direction.ASC, "ts.createdAt");
         case TITLE_ASC -> Sort.by(Sort.Direction.ASC, "title");
         case TITLE_DESC -> Sort.by(Sort.Direction.DESC, "title");
      };
      return primary.and(Sort.by("id"));
   }

   private static String escapeLike(String text) {
      return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
   }
}
