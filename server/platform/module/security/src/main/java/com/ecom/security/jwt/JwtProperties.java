package com.ecom.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How a service verifies access tokens, under {@code ecom.security.jwt}. Setting {@code issuer} turns verification on;
 * then one of {@code publicKey} or {@code jwksUri} is required.
 *
 * @param issuer            the {@code iss} claim tokens must carry
 * @param publicKey         a PEM string or a {@code classpath:}/{@code file:} location
 * @param jwksUri           the identity service's JWKS endpoint, as an alternative to a fixed public key
 * @param jwksMaxAgeSeconds how long fetched keys are used before they are fetched again
 */
@ConfigurationProperties(prefix = "ecom.security.jwt")
public record JwtProperties(String issuer, String publicKey, String jwksUri, @DefaultValue("600") long jwksMaxAgeSeconds) {
}
