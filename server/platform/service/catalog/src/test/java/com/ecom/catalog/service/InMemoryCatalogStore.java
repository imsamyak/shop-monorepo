package com.ecom.catalog.service;

import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.domain.entity.CatalogSku;
import com.ecom.catalog.port.out.persistence.LoadProductPort;
import com.ecom.catalog.port.out.persistence.LoadSkuPort;
import com.ecom.catalog.port.out.persistence.SaveProductPort;
import com.ecom.catalog.port.out.persistence.SaveSkuPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A fake of the persistence ports with the same contract as the MongoDB adapters: a save only succeeds when it is based
 * on the stored revision. {@link #conflictOnNextSaves} makes saves fail like a concurrent writer got there first.
 */
class InMemoryCatalogStore implements LoadProductPort, SaveProductPort, LoadSkuPort, SaveSkuPort {

   final Map<String, CatalogProduct> products = new HashMap<>();
   final Map<String, CatalogSku> skus = new HashMap<>();
   int productSaves;
   int skuSaves;
   private int conflicts;

   void conflictOnNextSaves(int count) {
      conflicts = count;
   }

   @Override
   public Optional<CatalogProduct> loadById(String productId) {
      return Optional.ofNullable(products.get(productId));
   }

   @Override
   public boolean saveIfUnchanged(CatalogProduct product) {
      CatalogProduct stored = products.get(product.id());
      if (conflicts > 0 || (stored == null ? 0 : stored.revision()) != product.revision()) {
         conflicts = Math.max(0, conflicts - 1);
         return false;
      }
      products.put(product.id(), product.toBuilder().revision(product.revision() + 1).build());
      productSaves++;
      return true;
   }

   @Override
   public Optional<CatalogSku> loadBySkuId(String skuId) {
      return Optional.ofNullable(skus.get(skuId));
   }

   @Override
   public List<CatalogSku> loadByProductId(String productId) {
      return new ArrayList<>(skus.values().stream().filter(sku -> productId.equals(sku.productId())).toList());
   }

   @Override
   public boolean saveIfUnchanged(CatalogSku sku) {
      CatalogSku stored = skus.get(sku.skuId());
      if (conflicts > 0 || (stored == null ? 0 : stored.revision()) != sku.revision()) {
         conflicts = Math.max(0, conflicts - 1);
         return false;
      }
      skus.put(sku.skuId(), sku.toBuilder().revision(sku.revision() + 1).build());
      skuSaves++;
      return true;
   }
}
