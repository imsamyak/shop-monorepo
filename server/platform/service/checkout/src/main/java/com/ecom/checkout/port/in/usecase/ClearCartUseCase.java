package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.ClearCartCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface ClearCartUseCase extends CommandUseCase<ClearCartCmd, CartResult> {
}
