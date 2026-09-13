package com.ecom.listing.domain.converter;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuPropertyMapConverterTest {

   private final SkuPropertyMapConverter converter = new SkuPropertyMapConverter();

   @Test
   void sortsKeysSoEqualSetsProduceTheSameColumnValue() {
      TreeMap<String, String> a = new TreeMap<>();
      a.put("size", "XL");
      a.put("color", "red");

      assertThat(converter.convertToDatabaseColumn(a)).isEqualTo("color:red,size:XL");
   }

   @Test
   void roundTripsValuesContainingAColon() {
      TreeMap<String, String> original = new TreeMap<>();
      original.put("url", "http://x.com/a");
      original.put("color", "red");

      String stored = converter.convertToDatabaseColumn(original);

      assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(original);
   }

   @Test
   void emptyAndNullMapToEmptyColumnAndBack() {
      assertThat(converter.convertToDatabaseColumn(null)).isEmpty();
      assertThat(converter.convertToDatabaseColumn(new TreeMap<>())).isEmpty();
      assertThat(converter.convertToEntityAttribute(null)).isEmpty();
      assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
   }

   @Test
   void refusesToWriteValuesItCouldNotReadBack() {
      TreeMap<String, String> commaInValue = new TreeMap<>();
      commaInValue.put("tag", "a,b");
      TreeMap<String, String> colonInKey = new TreeMap<>();
      colonInKey.put("a:b", "x");

      assertThatThrownBy(() -> converter.convertToDatabaseColumn(commaInValue)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> converter.convertToDatabaseColumn(colonInKey)).isInstanceOf(IllegalArgumentException.class);
   }
}
