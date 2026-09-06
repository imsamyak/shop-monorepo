package com.ecom.listing.service.sku;

import com.ecom.listing.domain.exception.sku.SkuNotFoundException;
import com.ecom.listing.port.in.usecase.sku.GetSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.query.GetSkuQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetSkuService implements GetSkuUseCase {

   private final LoadSkuPort loadSkuPort;
   private final SkuMapper skuMapper;

   @Override
   public SkuResult execute(GetSkuQuery query) {
      return loadSkuPort.load(query.productId(), query.skuId())
            .map(skuMapper::toResult)
            .orElseThrow(() -> SkuNotFoundException.of(query.productId(), query.skuId()));
   }
}
