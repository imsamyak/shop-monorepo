package com.ecom.listing.domain.exception.product;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductNotFoundException extends EntityNotFoundException {

   private final UUID productId;

   public ProductNotFoundException(UUID productId) {
      super("Product", productId);
      this.productId = productId;
   }

   public static ProductNotFoundException of(UUID productId) {
      return new ProductNotFoundException(productId);
   }
}
