package com.ecom.listing.service.sku;

import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.port.in.usecase.sku.ListSkusUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.query.ListSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ListSkusService implements ListSkusUseCase {

   private final LoadProductPort loadProductPort;
   private final LoadSkuPort loadSkuPort;
   private final SkuMapper skuMapper;

   @Override
   public SkuListResult execute(ListSkusQuery query) {
      loadProductPort.load(query.productId())
            .orElseThrow(() -> ProductNotFoundException.of(query.productId()));
      return new SkuListResult(skuMapper.toResultList(loadSkuPort.loadByProduct(query.productId())));
   }
}
