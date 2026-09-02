package com.ecom.checkout.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CommitCheckoutCmd(
      @NotNull UUID checkoutId
) {
}
