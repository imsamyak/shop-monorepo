package com.ecom.identity.adapter.in.web.controller.dto.response;

import com.ecom.identity.port.in.usecase.dto.result.UserResult;

public record UserResponse(String id, String email, String role, String sellerId) {
   public static UserResponse from(UserResult result) {
      return new UserResponse(result.id(), result.email(), result.role(), result.sellerId());
   }
}
