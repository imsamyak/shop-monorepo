package com.ecom.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The authenticated caller. How it was established (gateway, token, headers) is not this module's concern.
 */
public record UserPrincipal(
    String userId,
    String email,
    Set<UserRole> roles,
    String sellerId
) {

   public UserPrincipal {
      if (roles == null) {
         roles = Collections.emptySet();
      } else {
         roles = Collections.unmodifiableSet(new HashSet<>(roles));
      }
   }

   public boolean hasRole(UserRole role) {
      return roles.contains(role);
   }

   public boolean hasAnyRole(UserRole... requiredRoles) {
      if (roles.isEmpty() || requiredRoles == null) return false;
      for (UserRole r : requiredRoles) {
         if (roles.contains(r)) return true;
      }
      return false;
   }

   public static UserPrincipal of(String userId, String email, UserRole role, String sellerId) {
      return new UserPrincipal(userId, email, Collections.singleton(role), sellerId);
   }
}
