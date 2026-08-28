package com.ecom.security;

import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.shared.exception.security.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardTest {

   private final UUID userId = UUID.randomUUID();
   private final UUID sellerId = UUID.randomUUID();

   @AfterEach
   void clear() {
      SecurityContext.clear();
   }

   private void callerIs(UserRole role, UUID user, UUID seller) {
      SecurityContext.setPrincipal(new UserPrincipal(
          user.toString(), "a@b.c", Set.of(role), seller == null ? null : seller.toString()));
   }

   @Test
   void anonymousCallerIsUnauthorized() {
      assertThatThrownBy(Caller::principal).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> OwnershipGuard.requireUser(userId)).isInstanceOf(UnauthorizedException.class);
      assertThatThrownBy(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN)).isInstanceOf(UnauthorizedException.class);
   }

   @Test
   void userOwnershipAcceptsOnlyTheOwner() {
      callerIs(UserRole.ROLE_BUYER, userId, null);
      assertThatCode(() -> OwnershipGuard.requireUser(userId)).doesNotThrowAnyException();
      assertThatThrownBy(() -> OwnershipGuard.requireUser(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void adminDoesNotPassUserOwnershipUnlessNamed() {
      callerIs(UserRole.ROLE_ADMIN, UUID.randomUUID(), null);
      assertThatThrownBy(() -> OwnershipGuard.requireUser(userId)).isInstanceOf(ForbiddenException.class);
      assertThatCode(() -> OwnershipGuard.requireUserOr(userId, UserRole.ROLE_ADMIN)).doesNotThrowAnyException();
      assertThatThrownBy(() -> OwnershipGuard.requireUserOr(userId, UserRole.ROLE_SYSTEM))
          .isInstanceOf(ForbiddenException.class);
   }

   @Test
   void sellerOwnershipNeedsSellerRoleAndMatchingSellerId() {
      callerIs(UserRole.ROLE_SELLER, userId, sellerId);
      assertThatCode(() -> OwnershipGuard.requireSeller(sellerId)).doesNotThrowAnyException();
      assertThatThrownBy(() -> OwnershipGuard.requireSeller(UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
      assertThatThrownBy(() -> OwnershipGuard.requireSeller(null)).isInstanceOf(ForbiddenException.class);

      callerIs(UserRole.ROLE_BUYER, userId, sellerId);
      assertThatThrownBy(() -> OwnershipGuard.requireSeller(sellerId)).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void adminDoesNotPassSellerOwnershipUnlessNamed() {
      callerIs(UserRole.ROLE_ADMIN, userId, null);
      assertThatThrownBy(() -> OwnershipGuard.requireSeller(sellerId)).isInstanceOf(ForbiddenException.class);
      assertThatCode(() -> OwnershipGuard.requireSellerOr(sellerId, UserRole.ROLE_ADMIN)).doesNotThrowAnyException();
   }

   @Test
   void roleGuardAcceptsOnlyListedRoles() {
      callerIs(UserRole.ROLE_SYSTEM, userId, null);
      assertThatCode(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN, UserRole.ROLE_SYSTEM)).doesNotThrowAnyException();
      assertThatThrownBy(() -> RoleGuard.requireAnyRole(UserRole.ROLE_ADMIN)).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void callerReadsIdsAndRejectsNonSellers() {
      callerIs(UserRole.ROLE_BUYER, userId, null);
      assertThat(Caller.userId()).isEqualTo(userId);
      assertThat(Caller.hasRole(UserRole.ROLE_BUYER)).isTrue();
      assertThatThrownBy(Caller::sellerId).isInstanceOf(ForbiddenException.class);

      callerIs(UserRole.ROLE_SELLER, userId, sellerId);
      assertThat(Caller.sellerId()).isEqualTo(sellerId);
   }
}
