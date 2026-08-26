package com.ecom.identity.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

public record ServiceTokenResult(String accessToken, long expiresIn) implements Result.One {
}
