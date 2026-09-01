package com.ecom.inventory.adapter.in.web.controller.item.dto.response;

import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;

public record InventoryResponse(
      Long skuId,
      int totalQuantity,
      int reservedQuantity,
      int availableQuantity
) {
   public static InventoryResponse from(InventoryResult result) {
      if (result == null) {
         return null;
      }
      return new InventoryResponse(
            result.skuId(),
            result.totalQuantity(),
            result.reservedQuantity(),
            result.availableQuantity()
      );
   }
}
