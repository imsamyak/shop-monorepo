package com.ecom.security;

import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.shared.exception.security.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityTest {

   static class Controller {
      @RequireRole(UserRole.ROLE_SELLER)
      public void sellersOnly() {
      }

      public void open() {
      }
   }

   private final SecurityInterceptor interceptor = new SecurityInterceptor();

   @AfterEach
   void clear() {
      SecurityContext.clear();
   }

   private boolean call(String method) throws Exception {
      HandlerMethod handler = new HandlerMethod(new Controller(), Controller.class.getMethod(method));
      return interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler);
   }

   @Test
   void interceptorAllowsEndpointWithoutRequireRole() throws Exception {
      assertThat(call("open")).isTrue();
   }

   @Test
   void interceptorRejectsAnonymousCallerWith401() {
      assertThatThrownBy(() -> call("sellersOnly")).isInstanceOf(UnauthorizedException.class);
   }

   @Test
   void interceptorRejectsWrongRoleWith403() {
      SecurityContext.setPrincipal(UserPrincipal.of("u1", "a@b.c", UserRole.ROLE_BUYER, null));
      assertThatThrownBy(() -> call("sellersOnly")).isInstanceOf(ForbiddenException.class);
   }

   @Test
   void interceptorAllowsMatchingRole() throws Exception {
      SecurityContext.setPrincipal(UserPrincipal.of("u1", "a@b.c", UserRole.ROLE_SELLER, null));
      assertThat(call("sellersOnly")).isTrue();
   }

   @Test
   void filterSetsPrincipalDuringRequestAndClearsAfter() throws Exception {
      UserPrincipal caller = UserPrincipal.of("u1", "a@b.c", UserRole.ROLE_BUYER, null);
      PrincipalFilter filter = new PrincipalFilter(request -> Optional.of(caller));
      UserPrincipal[] seen = new UserPrincipal[1];

      filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
          new MockFilterChain() {
             @Override
             public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seen[0] = SecurityContext.getPrincipal();
             }
          });

      assertThat(seen[0]).isEqualTo(caller);
      assertThat(SecurityContext.isAuthenticated()).isFalse();
   }
}
