package com.ecom.listing.port.in.usecase.sku;

import com.ecom.listing.port.in.usecase.sku.dto.query.GetSkuQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetSkuUseCase extends QueryUseCase<GetSkuQuery, SkuResult> {
}
