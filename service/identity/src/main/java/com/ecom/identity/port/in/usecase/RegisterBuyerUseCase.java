package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.RegisterBuyerCmd;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.shared.usecase.CommandUseCase;

/** Self sign-up: always creates a buyer and signs them in. */
public interface RegisterBuyerUseCase extends CommandUseCase<RegisterBuyerCmd, SessionResult> {
}
