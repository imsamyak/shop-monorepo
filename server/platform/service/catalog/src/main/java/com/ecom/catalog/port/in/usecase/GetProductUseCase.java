package com.ecom.catalog.port.in.usecase;

import com.ecom.catalog.port.in.usecase.dto.GetProductQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetProductUseCase extends QueryUseCase<GetProductQuery, ProductResult> {
}
