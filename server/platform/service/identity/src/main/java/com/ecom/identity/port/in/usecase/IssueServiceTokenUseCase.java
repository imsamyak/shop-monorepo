package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.command.IssueServiceTokenCmd;
import com.ecom.identity.port.in.usecase.dto.result.ServiceTokenResult;
import com.ecom.shared.usecase.CommandUseCase;

/** Client credentials: a configured internal service exchanges its secret for a short-lived ROLE_SYSTEM token. */
public interface IssueServiceTokenUseCase extends CommandUseCase<IssueServiceTokenCmd, ServiceTokenResult> {
}
