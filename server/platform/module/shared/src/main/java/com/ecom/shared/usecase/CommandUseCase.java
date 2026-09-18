package com.ecom.shared.usecase;

import com.ecom.shared.dto.result.CommandResult;
import org.springframework.validation.annotation.Validated;

@Validated
public interface CommandUseCase<C, R extends CommandResult> extends UseCase<C, R> {


}
