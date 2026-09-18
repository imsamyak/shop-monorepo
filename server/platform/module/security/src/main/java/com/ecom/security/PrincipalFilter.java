package com.ecom.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Asks the {@link PrincipalResolver} who is calling and makes the answer available through {@link SecurityContext}
 * for the duration of the request. An anonymous request is let through; endpoints that need a role are rejected
 * later by {@link SecurityInterceptor}.
 */
public class PrincipalFilter extends OncePerRequestFilter {

   private final PrincipalResolver resolver;

   public PrincipalFilter(PrincipalResolver resolver) {
      this.resolver = resolver;
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
       throws ServletException, IOException {
      try {
         resolver.resolve(request).ifPresent(SecurityContext::setPrincipal);
         chain.doFilter(request, response);
      } finally {
         SecurityContext.clear();
      }
   }
}
