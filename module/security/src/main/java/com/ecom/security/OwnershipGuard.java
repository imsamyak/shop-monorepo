package com.ecom.security;

import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.shared.exception.security.UnauthorizedException;

import java.util.UUID;

/**
 * Ownership checks for application services.
 * <p>
 * The plain methods accept only the owner. Any other role that may also act on the resource (for example support
 * staff) must be named at the call site through the {@code ...Or} variants; nothing is allowed implicitly.
 */
public final class OwnershipGuard {

   private static final String NOT_OWNER = "You do not own this resource";

   private OwnershipGuard() {
   }

   /**
    * Passes only for the user whose id equals {@code ownerUserId}.
    */
   public static void requireUser(UUID ownerUserId) {
      requireUserOr(ownerUserId);
   }

   /**
    * Passes for the user whose id equals {@code ownerUserId}, or for a caller with one of {@code alsoAllowed}.
    *
    * @throws UnauthorizedException if the request is anonymous
    * @throws ForbiddenException    if none of the above applies
    */
   public static void requireUserOr(UUID ownerUserId, UserRole... alsoAllowed) {
      UserPrincipal principal = Caller.principal();
      boolean owner = ownerUserId != null && ownerUserId.toString().equalsIgnoreCase(principal.userId());
      if (!owner && !principal.hasAnyRole(alsoAllowed)) {
         throw ForbiddenException.of(NOT_OWNER);
      }
   }

   /**
    * Passes only for a seller whose seller id equals {@code ownerSellerId}.
    */
   public static void requireSeller(UUID ownerSellerId) {
      requireSellerOr(ownerSellerId);
   }

   /**
    * Passes for a seller whose seller id equals {@code ownerSellerId}, or for a caller with one of
    * {@code alsoAllowed}.
    *
    * @throws UnauthorizedException if the request is anonymous
    * @throws ForbiddenException    if none of the above applies
    */
   public static void requireSellerOr(UUID ownerSellerId, UserRole... alsoAllowed) {
      UserPrincipal principal = Caller.principal();
      boolean owner = ownerSellerId != null
          && principal.hasRole(UserRole.ROLE_SELLER)
          && ownerSellerId.toString().equalsIgnoreCase(principal.sellerId());
      if (!owner && !principal.hasAnyRole(alsoAllowed)) {
         throw ForbiddenException.of(NOT_OWNER);
      }
   }
}
