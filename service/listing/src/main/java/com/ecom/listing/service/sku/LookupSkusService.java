package com.ecom.listing.service.sku;

import com.ecom.listing.port.in.usecase.sku.LookupSkusUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.query.LookupSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class LookupSkusService implements LookupSkusUseCase {

   private final LoadSkuPort loadSkuPort;
   private final SkuMapper skuMapper;

   @Override
   public SkuListResult execute(LookupSkusQuery query) {
      return new SkuListResult(skuMapper.toResultList(loadSkuPort.loadAll(query.skuIds())));
   }
}
