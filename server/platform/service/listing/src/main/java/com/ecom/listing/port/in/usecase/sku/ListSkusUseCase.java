package com.ecom.listing.port.in.usecase.sku;

import com.ecom.listing.port.in.usecase.sku.dto.query.ListSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListSkusUseCase extends QueryUseCase<ListSkusQuery, SkuListResult> {
}
