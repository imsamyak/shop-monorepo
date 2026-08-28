package com.ecom.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * The one swappable part of authentication: works out who is calling from the incoming request,
 * or returns empty when the caller is anonymous. Implementations decide how the caller is established
 * (gateway token, forwarded claims, ...); everything else in this module only sees the {@link UserPrincipal}.
 * <p>
 * Without a {@code PrincipalResolver} bean, every request is anonymous.
 */
@FunctionalInterface
public interface PrincipalResolver {

   Optional<UserPrincipal> resolve(HttpServletRequest request);
}
