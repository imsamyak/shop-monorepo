package com.ecom.identity.port.out;

import java.time.Instant;

/**
 * Gets a reset link to the person who asked for it (in practice, by email). The service does not care how.
 */
public interface PasswordResetNotifier {

   void send(String email, String resetLink, Instant expiresAt);
}
