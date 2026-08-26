package com.ecom.identity.adapter.in.web.controller;

import com.ecom.security.jwt.RsaKeys;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Publishes the public half of the signing key so every other service can verify access tokens on its own. */
@RestController
public class JwksController {

   private final Map<String, Object> jwks;

   public JwksController(PublicKey verificationKey) {
      this.jwks = Map.of("keys", List.of(RsaKeys.toJwk(verificationKey)));
   }

   @GetMapping("/.well-known/jwks.json")
   public ResponseEntity<Map<String, Object>> keys() {
      return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(jwks);
   }
}
