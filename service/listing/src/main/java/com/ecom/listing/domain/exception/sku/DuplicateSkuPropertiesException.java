package com.ecom.listing.domain.exception.sku;

import com.ecom.shared.exception.conflict.DuplicateResourceException;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class DuplicateSkuPropertiesException extends DuplicateResourceException {

   private final UUID productId;
   private final Map<String, String> properties;

   public DuplicateSkuPropertiesException(UUID productId, Map<String, String> properties) {
      super("Sku", "properties", properties);
      this.productId = productId;
      this.properties = properties != null ? Map.copyOf(properties) : Map.of();
   }

   public static DuplicateSkuPropertiesException of(UUID productId, Map<String, String> properties) {
      return new DuplicateSkuPropertiesException(productId, properties);
   }
}
