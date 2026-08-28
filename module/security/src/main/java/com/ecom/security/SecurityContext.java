package com.ecom.security;

/**
 * Holds the caller of the request being handled on the current thread.
 * Set by {@link PrincipalFilter} and always cleared when the request ends.
 */
public final class SecurityContext {

   private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

   private SecurityContext() {
   }

   public static void setPrincipal(UserPrincipal principal) {
      CURRENT_USER.set(principal);
   }

   public static UserPrincipal getPrincipal() {
      return CURRENT_USER.get();
   }

   public static boolean isAuthenticated() {
      return CURRENT_USER.get() != null;
   }

   public static void clear() {
      CURRENT_USER.remove();
   }
}
