package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.validation.ValidationException;

public class CartLimitExceededException extends ValidationException {

   public CartLimitExceededException(String message) {
      super(message);
   }

   public static CartLimitExceededException tooManyItems(int max) {
      return new CartLimitExceededException("A cart can contain at most " + max + " different items");
   }

   public static CartLimitExceededException quantityTooLarge(Long skuId, int max) {
      return new CartLimitExceededException("Quantity of SKU " + skuId + " cannot exceed " + max);
   }
}
