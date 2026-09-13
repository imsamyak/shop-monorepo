package com.ecom.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

@ConfigurationProperties(prefix = "identity")
public record IdentityProperties(@DefaultValue Jwt jwt,
                                 @DefaultValue Lockout lockout,
                                 @DefaultValue PasswordReset passwordReset,
                                 @DefaultValue Map<String, String> serviceClients) {

   /**
    * {@code privateKey} is a PKCS#8 PEM (or a {@code classpath:}/{@code file:} location). Only when it is empty and
    * {@code devKeyFile} is set does the service generate a key and keep it in that file (local use only).
    */
   public record Jwt(String privateKey,
                     String devKeyFile,
                     @DefaultValue("ecom-identity") String issuer,
                     @DefaultValue("3600") long accessTtlSeconds,
                     @DefaultValue("300") long serviceTtlSeconds) {
   }

   public record Lockout(@DefaultValue("5") int maxAttempts,
                         @DefaultValue("900") long lockSeconds) {
   }

   /**
    * @param ttlSeconds  how long a reset link works
    * @param linkBaseUrl the page (usually in the storefront) that takes {@code ?token=...} and asks for the new password
    * @param logLinks    local use only: log the reset link, because no mail sender is wired yet
    */
   public record PasswordReset(@DefaultValue("1800") long ttlSeconds,
                               @DefaultValue("http://localhost:3000/reset-password") String linkBaseUrl,
                               @DefaultValue("false") boolean logLinks) {
   }
}
