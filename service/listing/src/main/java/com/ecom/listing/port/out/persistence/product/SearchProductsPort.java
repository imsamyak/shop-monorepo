package com.ecom.listing.port.out.persistence.product;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.port.in.usecase.product.dto.query.ProductSort;

import java.util.List;
import java.util.UUID;

public interface SearchProductsPort {

   /**
    * Live products matching the optional filters.
    *
    * @param sellerId only this seller's products, or {@code null}
    * @param title    case-insensitive substring of the title, or {@code null}
    * @param page     zero-based page index
    */
   Found search(UUID sellerId, String title, int page, int size, ProductSort sort);

   record Found(List<Product> items, long totalElements) {
   }
}
