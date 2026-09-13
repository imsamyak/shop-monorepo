package com.ecom.listing.port.out.persistence.sku;

import com.ecom.listing.domain.entity.Sku;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LoadSkuPort {

   Optional<Sku> load(UUID productId, Long skuId);

   List<Sku> load(UUID productId, Collection<Long> skuIds);

   /** The live SKUs among these ids, whatever product they belong to; unknown or deleted ids are left out. */
   List<Sku> loadAll(Collection<Long> skuIds);

   /** Every live SKU of the product. */
   List<Sku> loadByProduct(UUID productId);

   /**
     * Whether a live SKU of the product already has exactly these properties.
     *
     * @param excludingSkuId a SKU to ignore (the one being updated), or {@code null}
     */
   boolean existsWithProperties(UUID productId, Map<String, String> properties, Long excludingSkuId);
}
