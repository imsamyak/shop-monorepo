package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.CancelCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface CancelCheckoutUseCase extends CommandUseCase<CancelCheckoutCmd, CheckoutResult> {
}
