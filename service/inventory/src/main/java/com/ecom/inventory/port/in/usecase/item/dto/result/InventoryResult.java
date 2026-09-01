package com.ecom.inventory.port.in.usecase.item.dto.result;

import com.ecom.shared.dto.result.Result;
import lombok.Builder;

/**
 * @param quantityChange how much the total stock changed in this operation (negative when stock was removed)
 */
@Builder
public record InventoryResult(
      Long skuId,
      int totalQuantity,
      int reservedQuantity,
      int availableQuantity,
      int quantityChange
) implements Result.One {
}
