package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.RemoveCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface RemoveCartItemUseCase extends CommandUseCase<RemoveCartItemCmd, CartResult> {
}
