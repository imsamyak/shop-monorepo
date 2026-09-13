package com.ecom.inventory.port.in.usecase.item.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

/**
 * One page of the inventory listing. The cursor is the number of the next page, or null on the last page.
 */
public record InventoryPageResult(
      List<InventoryResult> items,
      int page,
      int size,
      long totalElements,
      int totalPages
) implements Result.Page<InventoryResult> {

   public InventoryPageResult {
      items = items == null ? List.of() : List.copyOf(items);
   }

   @Override
   public String cursor() {
      return page + 1 < totalPages ? String.valueOf(page + 1) : null;
   }
}
