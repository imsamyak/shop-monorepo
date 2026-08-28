package com.ecom.security;

import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.shared.exception.security.UnauthorizedException;

import java.util.Arrays;

/**
 * Role checks for application services, for rules that cannot be written as {@link RequireRole} on an endpoint.
 * Only the roles you list are accepted; no role passes by default.
 */
public final class RoleGuard {

   private RoleGuard() {
   }

   /**
    * Passes if the caller has at least one of the given roles.
    *
    * @throws UnauthorizedException if the request is anonymous
    * @throws ForbiddenException    if the caller has none of the roles
    */
   public static void requireAnyRole(UserRole... roles) {
      if (!Caller.principal().hasAnyRole(roles)) {
         throw ForbiddenException.of("One of these roles is required: " + Arrays.toString(roles));
      }
   }
}
