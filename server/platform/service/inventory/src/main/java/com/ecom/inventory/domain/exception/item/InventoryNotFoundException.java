package com.ecom.inventory.domain.exception.item;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

@Getter
public class InventoryNotFoundException extends EntityNotFoundException {

   private final Long skuId;

   public InventoryNotFoundException(Long skuId) {
      super("InventoryItem", skuId);
      this.skuId = skuId;
   }

   public static InventoryNotFoundException of(Long skuId) {
      return new InventoryNotFoundException(skuId);
   }
}
