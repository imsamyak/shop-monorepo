package com.ecom.identity.adapter.in.web.controller.dto.request;

import com.ecom.identity.port.in.usecase.dto.command.IssueServiceTokenCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceTokenRequest(
      @NotBlank @Size(max = 100) String clientId,
      @NotBlank @Size(max = 200) String clientSecret
) {
   public IssueServiceTokenCmd toCommand() {
      return new IssueServiceTokenCmd(clientId, clientSecret);
   }
}
