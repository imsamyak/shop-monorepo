package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.ForgotPasswordCmd;
import com.ecom.identity.port.in.usecase.dto.result.Done;
import com.ecom.shared.usecase.CommandUseCase;

/**
 * Sends a reset link to the address, if an account has it. The answer is the same either way, so nobody can use this
 * to find out which addresses are registered.
 */
public interface ForgotPasswordUseCase extends CommandUseCase<ForgotPasswordCmd, Done> {
}
