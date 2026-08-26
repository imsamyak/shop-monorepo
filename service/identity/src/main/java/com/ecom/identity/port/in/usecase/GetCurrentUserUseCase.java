package com.ecom.identity.port.in.usecase;

import com.ecom.identity.port.in.usecase.dto.query.GetCurrentUserQuery;
import com.ecom.identity.port.in.usecase.dto.result.UserResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetCurrentUserUseCase extends QueryUseCase<GetCurrentUserQuery, UserResult> {
}
