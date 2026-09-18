package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CheckoutNotFoundException extends EntityNotFoundException {

   private final UUID checkoutId;

   public CheckoutNotFoundException(UUID checkoutId) {
      super("Checkout", checkoutId);
      this.checkoutId = checkoutId;
   }

   public static CheckoutNotFoundException of(UUID checkoutId) {
      return new CheckoutNotFoundException(checkoutId);
   }
}
