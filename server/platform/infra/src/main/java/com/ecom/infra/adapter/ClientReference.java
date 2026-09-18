package com.ecom.infra.adapter;

import org.springframework.core.env.Environment;

/**
 * Which named client a module runs on, as the service wrote it: {@code ecom.<module>.client: <type>.<name>},
 * for example {@code ecom.outbox.client: kafka.core}.
 *
 * @param type the kind of client, for example {@code kafka}
 * @param name the name the profile gives it, for example {@code core}
 */
public record ClientReference(String type, String name) {

   /**
    * @throws IllegalStateException if the property is missing or is not of the form {@code <type>.<name>}
    */
   public static ClientReference of(Environment environment, String module) {
      String property = "ecom." + module + ".client";
      String value = environment.getProperty(property);
      int dot = value == null ? -1 : value.indexOf('.');
      if (dot <= 0 || dot == value.length() - 1) {
         throw new IllegalStateException(property + " must be '<type>.<name>', for example kafka.core, but was '"
             + value + "'");
      }
      return new ClientReference(value.substring(0, dot).trim().toLowerCase(), value.substring(dot + 1).trim());
   }
}
