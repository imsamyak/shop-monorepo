package com.ecom.identity.adapter.in.web.controller.dto.response;

import com.ecom.identity.port.in.usecase.dto.result.ServiceTokenResult;

public record ServiceTokenResponse(String accessToken, String tokenType, long expiresIn) {
   public static ServiceTokenResponse from(ServiceTokenResult result) {
      return new ServiceTokenResponse(result.accessToken(), "Bearer", result.expiresIn());
   }
}
