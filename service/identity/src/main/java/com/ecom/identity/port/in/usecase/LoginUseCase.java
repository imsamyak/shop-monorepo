package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.LoginCmd;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface LoginUseCase extends CommandUseCase<LoginCmd, SessionResult> {
}
