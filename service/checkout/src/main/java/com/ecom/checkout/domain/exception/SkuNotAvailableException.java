package com.ecom.checkout.domain.exception;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;

/** The SKU is unknown to the catalog (or was deleted), so it cannot be bought. */
@Getter
public class SkuNotAvailableException extends BusinessRuleException {

   private final Long skuId;

   public SkuNotAvailableException(Long skuId) {
      super(String.format("SKU %d is not available for purchase", skuId), Map.of("skuId", String.valueOf(skuId)));
      this.skuId = skuId;
   }

   public static SkuNotAvailableException of(Long skuId) {
      return new SkuNotAvailableException(skuId);
   }
}
