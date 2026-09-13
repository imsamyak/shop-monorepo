package com.ecom.identity.service.support;

import com.ecom.identity.config.IdentityProperties;
import com.ecom.identity.domain.User;
import com.ecom.identity.port.in.usecase.dto.result.SessionResult;
import com.ecom.identity.service.mapper.UserResultMapper;
import com.ecom.security.jwt.JwtSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Issues a session: one RS256 access token. Nothing is stored; when it expires the user signs in again. */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

   private final JwtSigner signer;
   private final IdentityProperties properties;

   public SessionResult start(User user) {
      long ttl = properties.jwt().accessTtlSeconds();
      String access = signer.sign(user.toPrincipal(), Duration.ofSeconds(ttl));
      return new SessionResult(access, ttl, UserResultMapper.toResult(user));
   }
}
