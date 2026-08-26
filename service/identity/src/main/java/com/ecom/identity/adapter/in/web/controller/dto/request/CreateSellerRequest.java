package com.ecom.identity.adapter.in.web.controller.dto.request;

import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.security.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSellerRequest(
      @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(max = 200) String password
) {
   /** The role is fixed here: the caller cannot ask for any other. */
   public CreateUserCmd toCommand() {
      return new CreateUserCmd(email, password, UserRole.ROLE_SELLER);
   }
}
