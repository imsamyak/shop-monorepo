package com.ecom.assetservice.port.in.usecase;

import com.ecom.assetservice.port.in.usecase.dto.command.DeleteImageCmd;
import com.ecom.shared.dto.result.Result;
import com.ecom.shared.usecase.CommandUseCase;

public interface DeleteImageUseCase extends CommandUseCase<DeleteImageCmd, Result.None> {
}
