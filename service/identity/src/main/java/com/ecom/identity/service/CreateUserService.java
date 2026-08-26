package com.ecom.identity.service;

import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.CreateUserUseCase;
import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.identity.port.in.usecase.dto.result.UserResult;
import com.ecom.identity.port.out.PasswordHasher;
import com.ecom.identity.port.out.UserStore;
import com.ecom.identity.service.mapper.UserResultMapper;
import com.ecom.identity.service.support.CredentialRules;
import com.ecom.shared.exception.conflict.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;

@Service
@Validated
@RequiredArgsConstructor
public class CreateUserService implements CreateUserUseCase {

   private final UserStore users;
   private final PasswordHasher hasher;
   private final Clock clock;

   @Override
   public UserResult execute(CreateUserCmd cmd) {
      String email = CredentialRules.normalizeEmail(cmd.email());
      CredentialRules.validatePassword(cmd.password());
      if (users.findByEmail(email).isPresent()) {
         throw DuplicateResourceException.of("User", "email", email);
      }
      try {
         User saved = users.save(User.create(email, hasher.hash(cmd.password()), cmd.role(), clock.instant()));
         return UserResultMapper.toResult(saved);
      } catch (DataIntegrityViolationException e) {
         throw DuplicateResourceException.of("User", "email", email); // lost a race with a parallel sign-up
      }
   }
}
