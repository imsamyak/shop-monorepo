package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.CommitCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface CommitCheckoutUseCase extends CommandUseCase<CommitCheckoutCmd, CheckoutResult> {
}
