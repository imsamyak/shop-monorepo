package com.ecom.checkout.domain.exception;

import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class InvalidCheckoutStateException extends BusinessRuleException {

   private final UUID checkoutId;
   private final CheckoutStatus currentStatus;
   private final String action;

   public InvalidCheckoutStateException(UUID checkoutId, CheckoutStatus currentStatus, String action) {
      super(
         String.format("Cannot %s checkout %s with status %s", action, checkoutId, currentStatus),
         Map.of(
            "checkoutId", checkoutId != null ? checkoutId.toString() : "null",
            "currentStatus", currentStatus != null ? currentStatus.name() : "null",
            "action", action != null ? action : "null"
         )
      );
      this.checkoutId = checkoutId;
      this.currentStatus = currentStatus;
      this.action = action;
   }

   public static InvalidCheckoutStateException of(UUID checkoutId, CheckoutStatus currentStatus, String action) {
      return new InvalidCheckoutStateException(checkoutId, currentStatus, action);
   }
}
