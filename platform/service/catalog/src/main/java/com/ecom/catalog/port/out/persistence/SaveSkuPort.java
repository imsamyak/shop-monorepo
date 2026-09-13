package com.ecom.catalog.port.out.persistence;

import com.ecom.catalog.domain.entity.CatalogSku;

public interface SaveSkuPort {

   /**
    * Stores the SKU in one atomic write that only succeeds if nobody stored it since it was loaded: the SKU's
    * {@code revision} must still be the stored one (0 for a SKU that is not stored yet).
    *
    * @return false if the SKU changed in the meantime; the caller reloads, applies the event again and retries
    */
   boolean saveIfUnchanged(CatalogSku sku);
}
