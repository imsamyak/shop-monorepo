package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.ResetPasswordCmd;
import com.ecom.identity.port.in.usecase.dto.result.Done;
import com.ecom.shared.usecase.CommandUseCase;

/**
 * Sets a new password for whoever holds a valid reset link. The link works once.
 */
public interface ResetPasswordUseCase extends CommandUseCase<ResetPasswordCmd, Done> {
}
