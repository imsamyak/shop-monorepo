package com.ecom.listing.port.in.usecase.sku;

import com.ecom.listing.port.in.usecase.sku.dto.query.LookupSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface LookupSkusUseCase extends QueryUseCase<LookupSkusQuery, SkuListResult> {
}
