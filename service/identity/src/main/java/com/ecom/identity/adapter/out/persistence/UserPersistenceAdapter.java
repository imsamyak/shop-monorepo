package com.ecom.identity.adapter.out.persistence;

import com.ecom.identity.domain.User;
import com.ecom.identity.port.out.UserStore;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class UserPersistenceAdapter implements UserStore {

   private final UserJpaRepository repository;

   UserPersistenceAdapter(UserJpaRepository repository) {
      this.repository = repository;
   }

   @Override
   public Optional<User> findByEmail(String email) {
      return repository.findByEmail(email).map(UserEntity::toDomain);
   }

   @Override
   public Optional<User> findById(UUID id) {
      return repository.findById(id).map(UserEntity::toDomain);
   }

   @Override
   public User save(User user) {
      UserEntity entity = repository.findById(user.id())
         .map(existing -> {
            existing.apply(user);
            return existing;
         })
         .orElseGet(() -> UserEntity.from(user));
      return repository.saveAndFlush(entity).toDomain();
   }
}
