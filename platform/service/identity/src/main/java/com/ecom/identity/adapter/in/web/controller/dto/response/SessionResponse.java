package com.ecom.identity.adapter.in.web.controller.dto.response;

import com.ecom.identity.port.in.usecase.dto.result.SessionResult;

/** The token is in the body because the caller is the storefront server, which turns it into an httpOnly cookie. */
public record SessionResponse(
      String accessToken,
      String tokenType,
      long expiresIn,
      UserResponse user
) {
   public static SessionResponse from(SessionResult result) {
      return new SessionResponse(result.accessToken(), "Bearer", result.accessExpiresIn(), UserResponse.from(result.user()));
   }
}
