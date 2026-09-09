package com.ecom.catalog.port.out.persistence;

import com.ecom.catalog.domain.entity.CatalogSku;

import java.util.List;
import java.util.Optional;

public interface LoadSkuPort {

   Optional<CatalogSku> loadBySkuId(String skuId);

   /**
    * Every SKU record that names this product, deleted ones included.
    */
   List<CatalogSku> loadByProductId(String productId);
}
