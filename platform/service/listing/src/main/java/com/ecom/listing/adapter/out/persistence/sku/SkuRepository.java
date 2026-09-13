package com.ecom.listing.adapter.out.persistence.sku;

import com.ecom.listing.domain.entity.Sku;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Deliberately a plain {@link Repository}: there is no {@code findById}, so a SKU can only be reached together with
 * the product it belongs to.
 */
interface SkuRepository extends Repository<Sku, Long> {

   @Query("SELECT s FROM Sku s WHERE s.id = :id AND s.product.id = :productId")
   Optional<Sku> findByIdAndProductId(@Param("id") Long id, @Param("productId") UUID productId);

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT s FROM Sku s WHERE s.id = :id AND s.product.id = :productId")
   Optional<Sku> findByIdAndProductIdForUpdate(@Param("id") Long id, @Param("productId") UUID productId);

   @Query("SELECT s FROM Sku s WHERE s.product.id = :productId AND s.id IN :ids")
   List<Sku> findByProductIdAndIdIn(@Param("productId") UUID productId, @Param("ids") Collection<Long> ids);

   @Query("SELECT s FROM Sku s WHERE s.id IN :ids ORDER BY s.id")
   List<Sku> findAllByIdIn(@Param("ids") Collection<Long> ids);

   @Query("SELECT s FROM Sku s WHERE s.product.id = :productId ORDER BY s.id")
   List<Sku> findByProductId(@Param("productId") UUID productId);

   @Query("""
      SELECT COUNT(s) > 0 FROM Sku s
      WHERE s.product.id = :productId AND s.properties = :properties
          AND (:excludedId IS NULL OR s.id <> :excludedId)
   """)
   boolean existsWithProperties(@Param("productId") UUID productId,
                                 @Param("properties") TreeMap<String, String> properties,
                                 @Param("excludedId") Long excludedId);

   Sku saveAndFlush(Sku sku);
}
