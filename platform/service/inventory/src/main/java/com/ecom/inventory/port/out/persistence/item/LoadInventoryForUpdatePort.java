package com.ecom.inventory.port.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadInventoryForUpdatePort {

   Optional<InventoryItem> loadBySkuIdForUpdate(Long skuId);

   /**
     * Locks the items of the given SKUs in ascending SKU order (a fixed order prevents deadlocks between
     * concurrent multi-item requests). SKUs without an item are simply absent from the result.
     */
   List<InventoryItem> loadBySkuIdsForUpdate(Collection<Long> skuIds);
}
