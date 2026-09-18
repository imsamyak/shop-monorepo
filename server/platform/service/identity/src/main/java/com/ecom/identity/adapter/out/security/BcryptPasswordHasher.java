package com.ecom.identity.adapter.out.security;

import com.ecom.identity.port.out.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

   private final BCryptPasswordEncoder encoder;
   private final String dummyHash;

   public BcryptPasswordHasher(@Value("${identity.bcrypt-strength:12}") int strength) {
      this.encoder = new BCryptPasswordEncoder(strength);
      this.dummyHash = encoder.encode("not-a-real-password");
   }

   @Override
   public String hash(String rawPassword) {
      return encoder.encode(rawPassword);
   }

   @Override
   public boolean matches(String rawPassword, String hash) {
      return encoder.matches(rawPassword, hash);
   }

   @Override
   public void matchesDummy(String rawPassword) {
      encoder.matches(rawPassword, dummyHash);
   }
}
