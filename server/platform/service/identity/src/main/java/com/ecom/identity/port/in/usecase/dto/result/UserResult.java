package com.ecom.identity.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

public record UserResult(String id, String email, String role, String sellerId) implements Result.One {
}
