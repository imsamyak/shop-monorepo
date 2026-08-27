package com.ecom.shared.exception.conflict;

import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class DuplicateResourceException extends ConflictException {

   private final String resourceName;
   private final String field;
   private final Object value;

   public DuplicateResourceException(String resourceName, String field, Object value) {
      super(
          String.format("%s already exists with %s: %s", resourceName, field, value),
          ErrorCode.DUPLICATE_RESOURCE,
          Map.of(
              "resourceName", resourceName,
              "field", field,
              "value", String.valueOf(value)
          )
      );
      this.resourceName = resourceName;
      this.field = field;
      this.value = value;
   }

   @Builder
   public static DuplicateResourceException of(String resourceName, String field, Object value) {
      return new DuplicateResourceException(resourceName, field, value);
   }
}
