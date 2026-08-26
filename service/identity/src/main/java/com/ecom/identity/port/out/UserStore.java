package com.ecom.identity.port.out;

import com.ecom.identity.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserStore {

   Optional<User> findByEmail(String email);

   Optional<User> findById(UUID id);

   User save(User user);
}
