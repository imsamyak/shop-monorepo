package com.ecom.catalog.domain.exception;

import com.ecom.shared.exception.notfound.EntityNotFoundException;

public class ProductNotFoundException extends EntityNotFoundException {

   public ProductNotFoundException(String productId) {
      super("Product", productId);
   }
}
