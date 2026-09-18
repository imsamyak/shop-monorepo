package com.ecom.identity.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;

public record IssueServiceTokenCmd(
      @NotBlank String clientId,
      @NotBlank String clientSecret
) {
}
