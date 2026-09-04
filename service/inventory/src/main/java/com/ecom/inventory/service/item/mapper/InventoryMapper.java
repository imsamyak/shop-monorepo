package com.ecom.inventory.service.item.mapper;

import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

   public InventoryResult toResult(InventoryItem item, int quantityChange) {
      if (item == null) {
         return null;
      }
      return InventoryResult.builder()
            .skuId(item.getSkuId())
            .totalQuantity(item.getTotalQuantity())
            .reservedQuantity(item.getReservedQuantity())
            .availableQuantity(item.getAvailableQuantity())
            .quantityChange(quantityChange)
            .version(item.version())
            .build();
   }
}
