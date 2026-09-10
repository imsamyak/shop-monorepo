package com.ecom.identity.adapter.in.web.controller;

import com.ecom.identity.adapter.in.web.controller.dto.request.CreateSellerRequest;
import com.ecom.identity.adapter.in.web.controller.dto.request.ForgotPasswordRequest;
import com.ecom.identity.adapter.in.web.controller.dto.request.LoginRequest;
import com.ecom.identity.adapter.in.web.controller.dto.request.RegisterRequest;
import com.ecom.identity.adapter.in.web.controller.dto.request.ResetPasswordRequest;
import com.ecom.identity.adapter.in.web.controller.dto.request.ServiceTokenRequest;
import com.ecom.identity.adapter.in.web.controller.dto.response.ServiceTokenResponse;
import com.ecom.identity.adapter.in.web.controller.dto.response.SessionResponse;
import com.ecom.identity.adapter.in.web.controller.dto.response.UserResponse;
import com.ecom.identity.port.in.usecase.CreateUserUseCase;
import com.ecom.identity.port.in.usecase.ForgotPasswordUseCase;
import com.ecom.identity.port.in.usecase.GetCurrentUserUseCase;
import com.ecom.identity.port.in.usecase.IssueServiceTokenUseCase;
import com.ecom.identity.port.in.usecase.LoginUseCase;
import com.ecom.identity.port.in.usecase.RegisterBuyerUseCase;
import com.ecom.identity.port.in.usecase.RegisterSellerUseCase;
import com.ecom.identity.port.in.usecase.ResetPasswordUseCase;
import com.ecom.identity.port.in.usecase.dto.query.GetCurrentUserQuery;
import com.ecom.security.Caller;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nothing here is meant to be called from a browser directly (the storefront server calls it), so CORS is not enabled.
 */
@RestController
@RequiredArgsConstructor
public class IdentityController {

   private final RegisterBuyerUseCase registerBuyerUseCase;
   private final RegisterSellerUseCase registerSellerUseCase;
   private final LoginUseCase loginUseCase;
   private final CreateUserUseCase createUserUseCase;
   private final GetCurrentUserUseCase getCurrentUserUseCase;
   private final IssueServiceTokenUseCase issueServiceTokenUseCase;
   private final ForgotPasswordUseCase forgotPasswordUseCase;
   private final ResetPasswordUseCase resetPasswordUseCase;

   @PostMapping("/auth/register")
   @ResponseStatus(HttpStatus.CREATED)
   public SessionResponse register(@Valid @RequestBody RegisterRequest request) {
      return SessionResponse.from(registerBuyerUseCase.execute(request.toCommand()));
   }

   /** Anyone can sign up as a seller; the seller id in the token is created with the account. */
   @PostMapping("/auth/register/seller")
   @ResponseStatus(HttpStatus.CREATED)
   public SessionResponse registerSeller(@Valid @RequestBody RegisterRequest request) {
      return SessionResponse.from(registerSellerUseCase.execute(request.toSellerCommand()));
   }

   @PostMapping("/auth/login")
   public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
      return noStore(SessionResponse.from(loginUseCase.execute(request.toCommand())));
   }

   /**
    * Access tokens are stateless, so signing out means the caller discards its token (the storefront server clears
    * its cookie); there is nothing to revoke on the server until refresh tokens exist. It needs no token itself, so a
    * caller whose token has already expired can still sign out.
    */
   @PostMapping("/auth/logout")
   public ResponseEntity<Void> logout() {
      return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
   }

   @GetMapping("/auth/me")
   public UserResponse me() {
      return UserResponse.from(getCurrentUserUseCase.execute(new GetCurrentUserQuery(Caller.principal().userId())));
   }

   /**
    * Always 202, whether or not the address has an account.
    */
   @PostMapping("/auth/forgot-password")
   public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
      forgotPasswordUseCase.execute(request.toCommand());
      return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
   }

   @PostMapping("/auth/reset-password")
   public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
      resetPasswordUseCase.execute(request.toCommand());
      return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
   }

   @PostMapping("/auth/admin/sellers")
   @RequireRole(UserRole.ROLE_ADMIN)
   @ResponseStatus(HttpStatus.CREATED)
   public UserResponse createSeller(@Valid @RequestBody CreateSellerRequest request) {
      return UserResponse.from(createUserUseCase.execute(request.toCommand()));
   }

   /**
    * Client credentials for internal services (for example checkout calling inventory).
    */
   @PostMapping("/auth/token")
   public ResponseEntity<ServiceTokenResponse> serviceToken(@Valid @RequestBody ServiceTokenRequest request) {
      return noStore(ServiceTokenResponse.from(issueServiceTokenUseCase.execute(request.toCommand())));
   }

   private static <T> ResponseEntity<T> noStore(T body) {
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
   }
}
