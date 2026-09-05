package com.ecom.checkout.port.in.usecase;

import com.ecom.checkout.port.in.usecase.dto.command.AddCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface AddCartItemUseCase extends CommandUseCase<AddCartItemCmd, CartResult> {
}
