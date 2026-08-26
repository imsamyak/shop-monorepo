package com.ecom.identity.adapter.in.web.controller.dto.request;

import com.ecom.identity.port.in.usecase.dto.command.RegisterBuyerCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
      @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(max = 200) String password
) {
   public RegisterBuyerCmd toCommand() {
      return new RegisterBuyerCmd(email, password);
   }
}
