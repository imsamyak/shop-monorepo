package com.ecom.listing.service.product;

import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.port.in.usecase.product.GetProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.query.GetProductQuery;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.service.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetProductService implements GetProductUseCase {

   private final LoadProductPort loadProductPort;
   private final ProductMapper productMapper;

   @Override
   public ProductResult execute(GetProductQuery query) {
      return loadProductPort.load(query.id())
            .map(productMapper::toResult)
            .orElseThrow(() -> ProductNotFoundException.of(query.id()));
   }
}
