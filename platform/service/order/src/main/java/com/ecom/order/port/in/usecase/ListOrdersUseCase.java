package com.ecom.order.port.in.usecase;

import com.ecom.order.port.in.usecase.dto.query.ListOrdersQuery;
import com.ecom.order.port.in.usecase.dto.result.OrdersResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListOrdersUseCase extends QueryUseCase<ListOrdersQuery, OrdersResult> {
}
