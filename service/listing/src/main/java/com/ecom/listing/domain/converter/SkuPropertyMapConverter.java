package com.ecom.listing.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Stores SKU properties as {@code key:value,key:value} (keys sorted, so equal property sets always produce the same
 * column value, which the unique constraint relies on).
 * <p>
 * Keys must not contain {@code ,} or {@code :}, and values must not contain {@code ,} (a value may contain {@code :}
 * because the pair is split on the first colon only). The commands validate this; the converter additionally refuses
 * to write a value it could not read back.
 */
@Converter
public class SkuPropertyMapConverter implements AttributeConverter<TreeMap<String, String>, String> {

   public static final String ENTRY_SEPARATOR = ",";
   public static final String KEY_SEPARATOR = ":";

   @Override
   public String convertToDatabaseColumn(TreeMap<String, String> attribute) {
      if (attribute == null || attribute.isEmpty()) {
         return "";
      }
      return attribute.entrySet().stream()
            .map(entry -> {
               String key = entry.getKey();
               String value = entry.getValue();
               if (key == null || key.isBlank() || value == null || value.isBlank()
                     || key.contains(ENTRY_SEPARATOR) || key.contains(KEY_SEPARATOR)
                     || value.contains(ENTRY_SEPARATOR)) {
                  throw new IllegalArgumentException("Invalid SKU property '" + key + "'");
               }
               return key + KEY_SEPARATOR + value;
            })
            .collect(Collectors.joining(ENTRY_SEPARATOR));
   }

   @Override
   public TreeMap<String, String> convertToEntityAttribute(String dbData) {
      TreeMap<String, String> properties = new TreeMap<>();
      if (dbData == null || dbData.isBlank()) {
         return properties;
      }
      for (String pair : dbData.split(ENTRY_SEPARATOR)) {
         int separator = pair.indexOf(KEY_SEPARATOR);
         if (separator <= 0 || separator == pair.length() - 1) {
            continue;
         }
         properties.put(pair.substring(0, separator).strip(), pair.substring(separator + 1).strip());
      }
      return properties;
   }

   /** Convenience for callers that need the stored representation of a plain map (used in queries). */
   public static TreeMap<String, String> sorted(Map<String, String> properties) {
      return properties == null ? new TreeMap<>() : new TreeMap<>(properties);
   }
}
