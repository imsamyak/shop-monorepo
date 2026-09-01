package com.ecom.inventory.port.out.persistence.item;

public interface DeleteInventoryPort {

   void deleteBySkuId(Long skuId);
}
