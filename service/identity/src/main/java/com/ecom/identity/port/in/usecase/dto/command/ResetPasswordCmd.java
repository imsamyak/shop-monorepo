package com.ecom.identity.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordCmd(
   @NotBlank String token,
   @NotBlank String newPassword
) {
}
