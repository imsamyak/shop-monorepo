package com.ecom.inventory.port.out.persistence.item;

import com.ecom.inventory.domain.entity.InventoryItem;

import java.util.Optional;

public interface LoadInventoryPort {

   Optional<InventoryItem> loadBySkuId(Long skuId);
}
