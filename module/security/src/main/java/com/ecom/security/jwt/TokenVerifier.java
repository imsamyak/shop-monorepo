package com.ecom.security.jwt;

import com.ecom.security.UserPrincipal;

import java.util.Optional;

/**
 * Turns a bearer token into the authenticated user, or nothing when the token is missing, malformed, expired, issued
 * by someone else or signed with the wrong key.
 */
public interface TokenVerifier {

   Optional<UserPrincipal> verify(String bearerToken);
}
