package com.ecom.inventory.port.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoadInventoryPort {

   Optional<InventoryItem> loadBySkuId(Long skuId);

   /** The items that exist for the given sku ids, sorted by sku id. */
   List<InventoryItem> loadBySkuIds(Collection<Long> skuIds);

   /** One zero-based page of all items, sorted by sku id. */
   InventoryPage loadPage(int page, int size);
}
