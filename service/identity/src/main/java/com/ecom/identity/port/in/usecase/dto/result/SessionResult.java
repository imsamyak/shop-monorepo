package com.ecom.identity.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

/** What a sign-in or registration hands back. */
public record SessionResult(
      String accessToken,
      long accessExpiresIn,
      UserResult user
) implements Result.One {
}
