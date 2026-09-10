package com.ecom.order;

import com.ecom.security.UserPrincipal;
import com.ecom.security.UserRole;
import com.ecom.security.jwt.JwtSigner;
import com.ecom.security.jwt.RsaKeys;
import org.springframework.test.context.DynamicPropertyRegistry;

import java.security.KeyPair;
import java.time.Duration;
import java.util.UUID;

/**
 * Access tokens for the tests, signed with a key generated for the test run. The service verifies them like the ones
 * identity issues: only the public key is configured.
 */
public final class TestTokens {

   private static final KeyPair KEYS = RsaKeys.generate();
   private static final JwtSigner SIGNER = JwtSigner.forKey(KEYS.getPrivate(), "ecom-identity");

   private TestTokens() {
   }

   public static void register(DynamicPropertyRegistry registry) {
      registry.add("ecom.security.jwt.public-key", () -> RsaKeys.toPem(KEYS.getPublic()));
   }

   public static String seller(UUID sellerId) {
      return token(UserRole.ROLE_SELLER, UUID.randomUUID(), sellerId);
   }

   public static String buyer(UUID userId) {
      return token(UserRole.ROLE_BUYER, userId, null);
   }

   public static String admin() {
      return token(UserRole.ROLE_ADMIN, UUID.randomUUID(), null);
   }

   /** What another service (checkout calling inventory) presents. */
   public static String system() {
      return token(UserRole.ROLE_SYSTEM, UUID.randomUUID(), null);
   }

   private static String token(UserRole role, UUID userId, UUID sellerId) {
      return SIGNER.sign(UserPrincipal.of(userId.toString(), "test@example.com", role,
         sellerId == null ? null : sellerId.toString()), Duration.ofMinutes(5));
   }
}
