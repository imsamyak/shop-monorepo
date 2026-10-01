package com.ecom.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param apiPrefix  put in front of the path of every {@code @RestController} mapping; blank turns it off
 * @param healthPath where the liveness answer is served, outside the prefix and without signing in
 */
@ConfigurationProperties("ecom.web")
public record WebProperties(@DefaultValue("/api") String apiPrefix, @DefaultValue("/health") String healthPath) {
}
