package com.ecom.identity.adapter.in.web.controller.dto.request;

import com.ecom.identity.port.in.usecase.dto.command.LoginCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
      @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(max = 200) String password
) {
   public LoginCmd toCommand() {
      return new LoginCmd(email, password);
   }
}
