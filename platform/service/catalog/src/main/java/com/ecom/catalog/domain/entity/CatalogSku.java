package com.ecom.catalog.domain.entity;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the catalog knows about one SKU, built from {@code Sku.*} events and the {@code Inventory.*} snapshots. The two
 * sources are independent: each has its own version, and either can arrive first, so the SKU's own fields and its
 * stock are filled in separately. Every {@code with...} method returns {@code this} when the event changes nothing
 * (duplicate, older than what is stored, or after the SKU was deleted).
 *
 * @param revision     how often this record was stored; the store only accepts a save that was based on the latest one
 * @param version      the SKU's version of the last applied {@code Sku.Created/Updated}, null before any
 * @param deleted      the SKU was deleted; final, a late older event cannot bring it back
 * @param stockVersion the inventory item's version of the last applied snapshot, null before any
 */
@Builder(toBuilder = true)
public record CatalogSku(
      String skuId,
      String productId,
      long revision,
      Long version,
      boolean deleted,
      BigDecimal price,
      Map<String, String> properties,
      Long stockVersion,
      int availableStock
) {

   public static CatalogSku empty(String skuId) {
      return CatalogSku.builder().skuId(skuId).properties(Map.of()).build();
   }

   public CatalogSku withSku(String productId, BigDecimal price, Map<String, String> properties, long version) {
      if (deleted || (this.version != null && version <= this.version)) {
         return this;
      }
      return toBuilder()
            .productId(productId)
            .price(price)
            .properties(properties == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(properties)))
            .version(version)
            .build();
   }

   /**
    * Final whatever the version. Keeps the product the SKU belonged to so that product can be refreshed.
    */
   public CatalogSku withDeleted(String productId) {
      if (deleted) {
         return this;
      }
      return toBuilder().productId(productId != null ? productId : this.productId).deleted(true).build();
   }

   public CatalogSku withStock(int availableStock, long version) {
      if (stockVersion != null && version <= stockVersion) {
         return this;
      }
      return toBuilder().availableStock(availableStock).stockVersion(version).build();
   }

   public boolean inStock() {
      return !deleted && availableStock > 0;
   }
}
