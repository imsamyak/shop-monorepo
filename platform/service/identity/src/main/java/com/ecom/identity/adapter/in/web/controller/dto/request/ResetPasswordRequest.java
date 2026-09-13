package com.ecom.identity.adapter.in.web.controller.dto.request;

import com.ecom.identity.port.in.usecase.dto.command.ResetPasswordCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
   @NotBlank @Size(max = 200) String token,
   @NotBlank @Size(max = 200) String newPassword
) {

   public ResetPasswordCmd toCommand() {
      return new ResetPasswordCmd(token, newPassword);
   }
}
