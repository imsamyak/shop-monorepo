package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.DraftCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface DraftCheckoutUseCase extends CommandUseCase<DraftCheckoutCmd, CheckoutResult> {
}
