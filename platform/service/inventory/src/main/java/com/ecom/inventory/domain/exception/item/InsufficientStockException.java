package com.ecom.inventory.domain.exception.item;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;

@Getter
public class InsufficientStockException extends BusinessRuleException {

   private final Long skuId;
   private final int requested;
   private final int available;

   public InsufficientStockException(Long skuId, int requested, int available) {
      super(
         String.format("Insufficient stock for SKU %d: requested %d, available %d", skuId, requested, available),
         Map.of(
            "skuId", skuId != null ? String.valueOf(skuId) : "null",
            "requested", requested,
            "available", available
         )
      );
      this.skuId = skuId;
      this.requested = requested;
      this.available = available;
   }

   public static InsufficientStockException of(Long skuId, int requested, int available) {
      return new InsufficientStockException(skuId, requested, available);
   }
}
