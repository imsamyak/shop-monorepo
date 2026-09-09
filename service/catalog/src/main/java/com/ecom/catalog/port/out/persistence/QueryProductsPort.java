package com.ecom.catalog.port.out.persistence;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.domain.entity.ProductSort;

import java.util.List;
import java.util.Optional;

/**
 * Reads for the home page. Only visible products (created and not deleted) are ever returned.
 */
public interface QueryProductsPort {

   Optional<CatalogProduct> findVisibleById(String productId);

   /**
    * @param availability only products with this availability, or null for all
    * @param page         zero-based
    */
   Found findVisible(Availability availability, ProductSort sort, int page, int size);

   record Found(List<CatalogProduct> products, long total) {
   }
}
