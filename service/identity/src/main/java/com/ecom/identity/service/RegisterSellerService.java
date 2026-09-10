package com.ecom.identity.service;

import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.CreateUserUseCase;
import com.ecom.identity.port.in.usecase.RegisterSellerUseCase;
import com.ecom.identity.port.in.usecase.dto.command.CreateUserCmd;
import com.ecom.identity.port.in.usecase.dto.command.RegisterSellerCmd;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.identity.port.in.usecase.dto.result.UserResult;
import com.ecom.identity.port.out.UserStore;
import com.ecom.identity.service.support.SessionIssuer;
import com.ecom.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class RegisterSellerService implements RegisterSellerUseCase {

   private final CreateUserUseCase createUser;
   private final UserStore users;
   private final SessionIssuer sessions;

   @Override
   public SessionResult execute(RegisterSellerCmd cmd) {
      UserResult created = createUser.execute(new CreateUserCmd(cmd.email(), cmd.password(), UserRole.ROLE_SELLER));
      User user = users.findById(UUID.fromString(created.id())).orElseThrow();
      return sessions.start(user);
   }
}
