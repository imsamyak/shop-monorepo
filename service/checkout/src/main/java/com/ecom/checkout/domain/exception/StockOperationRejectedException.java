package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;

/** Inventory understood the request but refused it (insufficient stock, expired reservation, unknown SKU, ...). */
@Getter
public class StockOperationRejectedException extends BusinessRuleException {

   private final String operation;

   public StockOperationRejectedException(String operation, String reason) {
      super(String.format("Inventory rejected %s: %s", operation, reason),
            Map.of("operation", operation, "reason", reason));
      this.operation = operation;
   }

   public static StockOperationRejectedException of(String operation, String reason) {
      return new StockOperationRejectedException(operation, reason);
   }
}
