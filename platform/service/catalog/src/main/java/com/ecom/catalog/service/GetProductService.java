package com.ecom.catalog.service;

import com.ecom.catalog.domain.exception.ProductNotFoundException;
import com.ecom.catalog.port.in.usecase.GetProductUseCase;
import com.ecom.catalog.port.in.usecase.dto.GetProductQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductResult;
import com.ecom.catalog.port.out.persistence.QueryProductsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * One product of the home page; unknown and deleted products are both "not found".
 */
@Service
@Validated
@RequiredArgsConstructor
public class GetProductService implements GetProductUseCase {

   private final QueryProductsPort queryProductsPort;

   @Override
   public ProductResult execute(GetProductQuery query) {
      return queryProductsPort.findVisibleById(query.productId())
            .map(CatalogMapper::toResult)
            .orElseThrow(() -> new ProductNotFoundException(query.productId()));
   }
}
