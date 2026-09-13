package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.validation.ValidationException;

public class CheckoutItemsRequiredException extends ValidationException {

   public CheckoutItemsRequiredException() {
      super("Provide items to check out or add items to the cart first");
   }

   public static CheckoutItemsRequiredException of() {
      return new CheckoutItemsRequiredException();
   }
}
