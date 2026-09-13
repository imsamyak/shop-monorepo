package com.ecom.shared.dto.patch;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Jakarta Bean Validation ValueExtractor for {@link Patch}.
 * Enables container element validation such as {@code Patch<@Size(max = 200) String>}.
 */
public class PatchValueExtractor implements ValueExtractor<Patch<@ExtractedValue ?>> {

   @Override
   public void extractValues(Patch<?> originalValue, ValueReceiver receiver) {
      if (originalValue != null && originalValue.isSpecified()) {
         receiver.value(null, originalValue.value());
      }
   }
}
