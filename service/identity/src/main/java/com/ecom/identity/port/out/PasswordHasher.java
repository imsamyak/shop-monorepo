package com.ecom.identity.port.out;

public interface PasswordHasher {

   String hash(String rawPassword);

   boolean matches(String rawPassword, String hash);

   /** Burns the same time as a real check, so an unknown email is not distinguishable by response time. */
   void matchesDummy(String rawPassword);
}
