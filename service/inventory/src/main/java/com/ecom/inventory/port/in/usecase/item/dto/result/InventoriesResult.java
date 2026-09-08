package com.ecom.inventory.port.in.usecase.item.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

public record InventoriesResult(List<InventoryResult> inventories) implements Result.Many<InventoryResult> {

   public InventoriesResult {
      inventories = inventories == null ? List.of() : List.copyOf(inventories);
   }

   @Override
   public List<InventoryResult> items() {
      return inventories;
   }
}
