package com.ecom.identity.config;

import com.ecom.security.jwt.JwtSigner;
import com.ecom.security.jwt.RsaJwtVerifier;
import com.ecom.security.jwt.RsaKeys;
import com.ecom.security.jwt.TokenVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;

@Slf4j
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {

   @Bean
   public Clock clock() {
      return Clock.systemUTC();
   }

   /**
    * The one place the signing key exists.
    */
   @Bean
   public PrivateKey signingKey(IdentityProperties properties) {
      IdentityProperties.Jwt jwt = properties.jwt();
      if (jwt.privateKey() != null && !jwt.privateKey().isBlank()) {
         return RsaKeys.parsePrivateKey(readPem(jwt.privateKey()));
      }
      if (jwt.devKeyFile() != null && !jwt.devKeyFile().isBlank()) {
         return loadOrCreateDevKey(Path.of(jwt.devKeyFile()));
      }
      throw new IllegalStateException("Set identity.jwt.private-key (a PKCS#8 PEM) so access tokens can be signed");
   }

   @Bean
   public JwtSigner jwtSigner(PrivateKey signingKey, IdentityProperties properties) {
      return JwtSigner.forKey(signingKey, properties.jwt().issuer());
   }

   @Bean
   public PublicKey verificationKey(PrivateKey signingKey) {
      return RsaKeys.publicFromPrivate(signingKey);
   }

   /**
    * The identity service verifies its own access tokens (for /auth/me and the admin endpoints) with its own public key.
    */
   @Bean
   public TokenVerifier tokenVerifier(PublicKey verificationKey, IdentityProperties properties) {
      return new RsaJwtVerifier(keyId -> verificationKey, properties.jwt().issuer());
   }

   private static PrivateKey loadOrCreateDevKey(Path file) {
      try {
         if (Files.exists(file)) {
            return RsaKeys.parsePrivateKey(Files.readString(file));
         }
         KeyPair pair = RsaKeys.generate();
         if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
         }
         Files.writeString(file, RsaKeys.toPem(pair.getPrivate()));
         log.warn("Generated a development signing key at {}. Production must set identity.jwt.private-key.", file);
         return pair.getPrivate();
      } catch (IOException e) {
         throw new IllegalStateException("Cannot read or write the development signing key " + file, e);
      }
   }

   private static String readPem(String value) {
      String trimmed = value.trim();
      if (trimmed.startsWith("classpath:") || trimmed.startsWith("file:")) {
         try (var in = new DefaultResourceLoader().getResource(trimmed).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
         } catch (IOException e) {
            throw new IllegalStateException("Cannot read the private key from " + trimmed, e);
         }
      }
      return trimmed;
   }
}
