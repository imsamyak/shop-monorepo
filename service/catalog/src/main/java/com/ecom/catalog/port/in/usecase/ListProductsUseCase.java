package com.ecom.catalog.port.in.usecase;

import com.ecom.catalog.port.in.usecase.dto.ListProductsQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductPageResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListProductsUseCase extends QueryUseCase<ListProductsQuery, ProductPageResult> {
}
