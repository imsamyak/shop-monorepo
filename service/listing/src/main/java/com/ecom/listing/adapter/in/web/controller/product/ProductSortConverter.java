package com.ecom.listing.adapter.in.web.controller.product;

import com.ecom.listing.port.in.usecase.product.dto.query.ProductSort;
import com.ecom.shared.exception.validation.InvalidCommandException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Reads {@code sort=field,direction} from the query string: field {@code createdAt} or {@code title}, for example
 * {@code title,asc}.
 */
@Component
class ProductSortConverter implements Converter<String, ProductSort> {

   @Override
   public ProductSort convert(String sort) {
      return switch (sort.strip().toLowerCase(Locale.ROOT)) {
         case "createdat,desc" -> ProductSort.CREATED_AT_DESC;
         case "createdat,asc", "createdat" -> ProductSort.CREATED_AT_ASC;
         case "title,asc", "title" -> ProductSort.TITLE_ASC;
         case "title,desc" -> ProductSort.TITLE_DESC;
         default -> throw InvalidCommandException.of(
            "Unsupported sort '" + sort + "'. Use createdAt or title, followed by ,asc or ,desc");
      };
   }
}
