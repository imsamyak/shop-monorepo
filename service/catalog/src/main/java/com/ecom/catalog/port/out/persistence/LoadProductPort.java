package com.ecom.catalog.port.out.persistence;

import com.ecom.catalog.domain.entity.CatalogProduct;

import java.util.Optional;

public interface LoadProductPort {

   /**
    * Any stored product, including deleted ones and ones whose {@code Created} event has not arrived yet.
    */
   Optional<CatalogProduct> loadById(String productId);
}
