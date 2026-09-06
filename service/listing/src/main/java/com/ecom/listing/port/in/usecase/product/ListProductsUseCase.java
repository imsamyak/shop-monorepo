package com.ecom.listing.port.in.usecase.product;

import com.ecom.listing.port.in.usecase.product.dto.query.ListProductsQuery;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductPageResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListProductsUseCase extends QueryUseCase<ListProductsQuery, ProductPageResult> {
}
