package com.ecom.identity.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;

public record RegisterBuyerCmd(
      @NotBlank String email,
      @NotBlank String password
) {
}
