package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.domain.BusinessRuleException;

public class EmptyCheckoutException extends BusinessRuleException {

   public EmptyCheckoutException() {
      super("Checkout must contain at least one item");
   }

   public static EmptyCheckoutException of() {
      return new EmptyCheckoutException();
   }
}
