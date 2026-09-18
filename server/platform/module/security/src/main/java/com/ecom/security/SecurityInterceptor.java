package com.ecom.security;

import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.shared.exception.security.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Enforces {@link RequireRole} on controller methods, falling back to the annotation on the controller class.
 */
public class SecurityInterceptor implements HandlerInterceptor {

   @Override
   public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
      if (!(handler instanceof HandlerMethod handlerMethod)) {
         return true;
      }

      RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
      if (requireRole == null) {
         requireRole = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RequireRole.class);
      }

      if (requireRole != null) {
         UserPrincipal principal = SecurityContext.getPrincipal();
         if (principal == null) {
            throw UnauthorizedException.of("Authentication required to access endpoint " + request.getRequestURI());
         }

         if (!principal.hasAnyRole(requireRole.value())) {
            throw ForbiddenException.of(String.format(
                "Access denied for user '%s'. Required roles: %s, Current roles: %s",
                principal.userId(),
                Arrays.toString(requireRole.value()),
                principal.roles()
            ));
         }
      }

      return true;
   }
}
