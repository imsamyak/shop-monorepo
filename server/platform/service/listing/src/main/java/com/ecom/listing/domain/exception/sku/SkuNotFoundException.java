package com.ecom.listing.domain.exception.sku;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SkuNotFoundException extends EntityNotFoundException {

   private final UUID productId;
   private final Long skuId;

   public SkuNotFoundException(UUID productId, Long skuId) {
      super("Sku", skuId);
      this.productId = productId;
      this.skuId = skuId;
   }

   public static SkuNotFoundException of(UUID productId, Long skuId) {
      return new SkuNotFoundException(productId, skuId);
   }

   public static SkuNotFoundException of(Long skuId) {
      return new SkuNotFoundException(null, skuId);
   }
}
