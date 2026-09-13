package com.ecom.listing.port.in.usecase.product;

import com.ecom.listing.port.in.usecase.product.dto.query.GetProductQuery;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetProductUseCase extends QueryUseCase<GetProductQuery, ProductResult> {
}
