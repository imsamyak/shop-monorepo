package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.GetCartQuery;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetCartUseCase extends QueryUseCase<GetCartQuery, CartResult> {
}
