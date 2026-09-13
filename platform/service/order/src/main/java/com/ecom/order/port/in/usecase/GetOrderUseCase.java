package com.ecom.order.port.in.usecase;

import com.ecom.order.port.in.usecase.dto.query.GetOrderQuery;
import com.ecom.order.port.in.usecase.dto.result.OrderResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetOrderUseCase extends QueryUseCase<GetOrderQuery, OrderResult> {
}
