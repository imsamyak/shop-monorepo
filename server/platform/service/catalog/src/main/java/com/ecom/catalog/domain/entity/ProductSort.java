package com.ecom.catalog.domain.entity;

import java.util.Arrays;

/**
 * The orders the home page can be listed in; ties are broken by product id so pages never overlap.
 */
public enum ProductSort {
   NEWEST("createdAt,desc"),
   OLDEST("createdAt,asc"),
   PRICE_ASC("price,asc"),
   PRICE_DESC("price,desc");

   private final String parameter;

   ProductSort(String parameter) {
      this.parameter = parameter;
   }

   /**
    * @return the order for a {@code sort} request parameter such as {@code price,asc}
    * @throws IllegalArgumentException if it names no supported order
    */
   public static ProductSort parse(String parameter) {
      String normalized = parameter.replace(" ", "");
      return Arrays.stream(values())
            .filter(sort -> sort.parameter.equalsIgnoreCase(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported sort '" + parameter + "'. Supported: "
                  + String.join(", ", Arrays.stream(values()).map(sort -> sort.parameter).toList())));
   }
}
