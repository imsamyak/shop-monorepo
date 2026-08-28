package com.ecom.security.jwt;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RSA key helpers shared by the identity service (which signs tokens) and every other service (which verifies them).
 * Keys are exchanged as PEM (PKCS#8 private, X.509 public) or as a JWK.
 */
public final class RsaKeys {

   public static final int MIN_KEY_BITS = 2048;

   private RsaKeys() {
   }

   public static KeyPair generate() {
      try {
         KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
         generator.initialize(MIN_KEY_BITS);
         return generator.generateKeyPair();
      } catch (GeneralSecurityException e) {
         throw new IllegalStateException("RSA is not available", e);
      }
   }

   public static PublicKey parsePublicKey(String pem) {
      try {
         byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
         RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
         requireStrong(key);
         return key;
      } catch (GeneralSecurityException | IllegalArgumentException e) {
         throw new IllegalStateException("The public key is not a valid RSA public key in PEM format", e);
      }
   }

   public static PrivateKey parsePrivateKey(String pem) {
      try {
         byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
         return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
      } catch (GeneralSecurityException | IllegalArgumentException e) {
         throw new IllegalStateException("The private key is not a valid RSA private key in PKCS#8 PEM format", e);
      }
   }

   /** The public half of a private key (RSA private keys in CRT form carry the public exponent). */
   public static PublicKey publicFromPrivate(PrivateKey privateKey) {
      if (!(privateKey instanceof RSAPrivateCrtKey crt)) {
         throw new IllegalStateException("Cannot derive the public key: the private key is not in CRT form");
      }
      try {
         return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
      } catch (GeneralSecurityException e) {
         throw new IllegalStateException(e);
      }
   }

   public static String toPem(PublicKey key) {
      return pem("PUBLIC KEY", key.getEncoded());
   }

   public static String toPem(PrivateKey key) {
      return pem("PRIVATE KEY", key.getEncoded());
   }

   /** A stable id for a key: the first 16 hex characters of the SHA-256 of its encoding. */
   public static String keyId(PublicKey key) {
      try {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getEncoded());
         return HexFormat.of().formatHex(digest, 0, 8);
      } catch (GeneralSecurityException e) {
         throw new IllegalStateException(e);
      }
   }

   /** The JWK representation published at /.well-known/jwks.json. */
   public static Map<String, Object> toJwk(PublicKey key) {
      RSAPublicKey rsa = (RSAPublicKey) key;
      Map<String, Object> jwk = new LinkedHashMap<>();
      jwk.put("kty", "RSA");
      jwk.put("use", "sig");
      jwk.put("alg", "RS256");
      jwk.put("kid", keyId(key));
      jwk.put("n", base64Url(rsa.getModulus()));
      jwk.put("e", base64Url(rsa.getPublicExponent()));
      return jwk;
   }

   public static PublicKey fromJwk(String modulus, String exponent) {
      try {
         BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
         BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
         RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
         requireStrong(key);
         return key;
      } catch (GeneralSecurityException | IllegalArgumentException ex) {
         throw new IllegalStateException("Invalid RSA JWK", ex);
      }
   }

   private static void requireStrong(RSAPublicKey key) {
      if (key.getModulus().bitLength() < MIN_KEY_BITS) {
         throw new IllegalStateException("RSA keys must be at least " + MIN_KEY_BITS + " bits");
      }
   }

   private static String base64Url(BigInteger value) {
      byte[] bytes = value.toByteArray();
      if (bytes.length > 1 && bytes[0] == 0) {
         bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   }

   private static String stripPem(String pem) {
      return pem.replaceAll("-----BEGIN [A-Z ]+-----", "").replaceAll("-----END [A-Z ]+-----", "").replaceAll("\\s", "");
   }

   private static String pem(String label, byte[] der) {
      String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
      return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
   }
}
