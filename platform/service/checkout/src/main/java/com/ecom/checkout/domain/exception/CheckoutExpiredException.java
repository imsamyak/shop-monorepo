package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
public class CheckoutExpiredException extends BusinessRuleException {

   private final UUID checkoutId;

   public CheckoutExpiredException(UUID checkoutId, Instant expiredAt) {
      super(String.format("Checkout %s expired at %s", checkoutId, expiredAt),
            Map.of("checkoutId", String.valueOf(checkoutId), "expiredAt", String.valueOf(expiredAt)));
      this.checkoutId = checkoutId;
   }

   public static CheckoutExpiredException of(UUID checkoutId, Instant expiredAt) {
      return new CheckoutExpiredException(checkoutId, expiredAt);
   }
}
