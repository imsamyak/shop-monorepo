package com.ecom.listing.port.in.usecase.product.dto.query;

/** The orderings a product listing supports; ties are always broken by id so paging is stable. */
public enum ProductSort {
   CREATED_AT_DESC,
   CREATED_AT_ASC,
   TITLE_ASC,
   TITLE_DESC
}
