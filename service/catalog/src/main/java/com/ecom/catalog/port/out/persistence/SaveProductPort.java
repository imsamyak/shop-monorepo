package com.ecom.catalog.port.out.persistence;

import com.ecom.catalog.domain.entity.CatalogProduct;

public interface SaveProductPort {

   /**
    * Stores the product in one atomic write that only succeeds if nobody stored it since it was loaded: the product's
    * {@code revision} must still be the stored one (0 for a product that is not stored yet).
    *
    * @return false if the product changed in the meantime; the caller reloads, applies the event again and retries
    */
   boolean saveIfUnchanged(CatalogProduct product);
}
