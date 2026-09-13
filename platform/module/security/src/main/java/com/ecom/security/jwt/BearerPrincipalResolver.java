package com.ecom.security.jwt;

import com.ecom.security.PrincipalResolver;
import com.ecom.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

/**
 * Establishes the caller from the {@code Authorization: Bearer} token only; no other header is ever trusted. A missing
 * or invalid token, or no {@link TokenVerifier} at all, leaves the request anonymous: endpoints that need a role are
 * then rejected by the interceptor.
 */
public class BearerPrincipalResolver implements PrincipalResolver {

   private static final String PREFIX = "Bearer ";

   private final ObjectProvider<TokenVerifier> verifier;

   public BearerPrincipalResolver(ObjectProvider<TokenVerifier> verifier) {
      this.verifier = verifier;
   }

   @Override
   public Optional<UserPrincipal> resolve(HttpServletRequest request) {
      String header = request.getHeader("Authorization");
      if (header == null || !header.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
         return Optional.empty();
      }
      TokenVerifier tokenVerifier = verifier.getIfAvailable();
      if (tokenVerifier == null) {
         return Optional.empty();
      }
      return tokenVerifier.verify(header.substring(PREFIX.length()).trim());
   }
}
