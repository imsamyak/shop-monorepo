package com.ecom.shared.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for the ObjectMapper used to (de)serialize messaging payloads. Dates are ISO-8601 strings and unknown
 * properties are ignored, so a producer can add a field without breaking existing consumers.
 */
public final class JsonMappers {

   private JsonMappers() {
   }

   public static ObjectMapper messaging() {
      return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
   }
}
