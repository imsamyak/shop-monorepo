package com.ecom.inventory.port.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;

public interface SaveInventoryPort {

   InventoryItem save(InventoryItem inventoryItem);
}
