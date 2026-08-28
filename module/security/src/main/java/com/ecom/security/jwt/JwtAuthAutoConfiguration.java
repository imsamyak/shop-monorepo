package com.ecom.security.jwt;

import com.ecom.security.PrincipalResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lets a service identify callers by their access token. The caller of a request is the one in its bearer token; a
 * service that configures nothing sees every request as anonymous. A {@link TokenVerifier} or {@link PrincipalResolver}
 * bean defined by the service replaces ours.
 */
@AutoConfiguration(beforeName = "com.ecom.security.SecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtAuthAutoConfiguration {

   /**
    * Verifies tokens with a configured public key, or with keys fetched from the identity service's JWKS endpoint.
    */
   @Bean
   @ConditionalOnMissingBean(TokenVerifier.class)
   @ConditionalOnProperty(prefix = "ecom.security.jwt", name = "issuer")
   TokenVerifier tokenVerifier(JwtProperties jwt) {
      RsaJwtVerifier.PublicKeyResolver keys;
      if (jwt.publicKey() != null && !jwt.publicKey().isBlank()) {
         var key = RsaKeys.parsePublicKey(readPem(jwt.publicKey()));
         keys = keyId -> key;
      } else if (jwt.jwksUri() != null && !jwt.jwksUri().isBlank()) {
         keys = new JwksPublicKeyResolver(jwt.jwksUri(), Duration.ofSeconds(jwt.jwksMaxAgeSeconds()));
      } else {
         throw new IllegalStateException(
             "Set ecom.security.jwt.public-key or ecom.security.jwt.jwks-uri so access tokens can be verified");
      }
      return new RsaJwtVerifier(keys, jwt.issuer());
   }

   @Bean
   @ConditionalOnMissingBean(PrincipalResolver.class)
   PrincipalResolver bearerPrincipalResolver(ObjectProvider<TokenVerifier> verifier) {
      return new BearerPrincipalResolver(verifier);
   }

   private static String readPem(String value) {
      String trimmed = value.trim();
      if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
         Resource resource = new DefaultResourceLoader().getResource(trimmed);
         try (var in = resource.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
         } catch (IOException e) {
            throw new IllegalStateException("Cannot read the public key from " + trimmed, e);
         }
      }
      return trimmed;
   }
}
