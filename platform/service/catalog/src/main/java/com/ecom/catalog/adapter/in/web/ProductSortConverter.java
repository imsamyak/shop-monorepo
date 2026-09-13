package com.ecom.catalog.adapter.in.web;

import com.ecom.catalog.domain.entity.ProductSort;
import com.ecom.shared.exception.validation.ValidationException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Reads {@code sort=createdAt,desc} (or {@code createdAt,asc}, {@code price,asc}, {@code price,desc}) from the query
 * string.
 */
@Component
class ProductSortConverter implements Converter<String, ProductSort> {

   @Override
   public ProductSort convert(String sort) {
      try {
         return ProductSort.parse(sort);
      } catch (IllegalArgumentException e) {
         throw new ValidationException(e.getMessage());
      }
   }
}
