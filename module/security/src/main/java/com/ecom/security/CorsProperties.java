package com.ecom.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Which websites may call the API from a browser, under {@code ecom.security.cors}.
 *
 * @param allowedOrigins origins such as {@code https://shop.example.com}, or {@code *} for every site (the default,
 *                       until the storefront's address is known). Access tokens travel in the {@code Authorization}
 *                       header, never in cookies, so allowing every origin does not let another site act as a user.
 */
@ConfigurationProperties(prefix = "ecom.security.cors")
public record CorsProperties(@DefaultValue("*") List<String> allowedOrigins) {
}
