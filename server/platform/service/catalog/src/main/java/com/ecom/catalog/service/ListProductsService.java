package com.ecom.catalog.service;

import com.ecom.catalog.port.in.usecase.ListProductsUseCase;
import com.ecom.catalog.port.in.usecase.dto.ListProductsQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductPageResult;
import com.ecom.catalog.port.out.persistence.QueryProductsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * The home page feed: one page of visible products in the requested order, optionally only the available ones.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ListProductsService implements ListProductsUseCase {

   private final QueryProductsPort queryProductsPort;

   @Override
   public ProductPageResult execute(ListProductsQuery query) {
      QueryProductsPort.Found found =
            queryProductsPort.findVisible(query.availability(), query.sort(), query.page(), query.size());
      int totalPages = (int) ((found.total() + query.size() - 1) / query.size());
      return new ProductPageResult(
            found.products().stream().map(CatalogMapper::toResult).toList(),
            query.page(), query.size(), found.total(), totalPages);
   }
}
