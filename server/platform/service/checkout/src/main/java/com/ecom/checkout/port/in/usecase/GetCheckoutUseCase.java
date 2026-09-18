package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.GetCheckoutQuery;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetCheckoutUseCase extends QueryUseCase<GetCheckoutQuery, CheckoutResult> {
}
