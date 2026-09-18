package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.SetCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface SetCartItemUseCase extends CommandUseCase<SetCartItemCmd, CartResult> {
}
