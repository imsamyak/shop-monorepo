package com.ecom.identity.port.in.usecase.dto.query;

import jakarta.validation.constraints.NotBlank;

public record GetCurrentUserQuery(@NotBlank String userId) {
}
