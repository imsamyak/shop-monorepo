package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.identity.port.in.usecase.dto.result.UserResult;
import com.ecom.shared.usecase.CommandUseCase;

/** Creates an account with the given role. Used for administrator-created sellers and for seeding. */
public interface CreateUserUseCase extends CommandUseCase<CreateUserCmd, UserResult> {
}
