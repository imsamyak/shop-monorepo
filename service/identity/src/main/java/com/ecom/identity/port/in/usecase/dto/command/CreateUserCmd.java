package com.ecom.identity.port.in.usecase.dto.command;

import com.ecom.security.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserCmd(
      @NotBlank String email,
      @NotBlank String password,
      @NotNull UserRole role
) {
}
