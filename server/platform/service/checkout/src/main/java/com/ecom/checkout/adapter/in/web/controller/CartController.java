package com.ecom.checkout.adapter.in.web.controller;

import com.ecom.checkout.adapter.in.web.controller.dto.request.AddCartItemRequest;
import com.ecom.checkout.adapter.in.web.controller.dto.request.SetCartItemRequest;
import com.ecom.checkout.adapter.in.web.controller.dto.response.CartResponse;
import com.ecom.checkout.port.in.usecase.AddCartItemUseCase;
import com.ecom.checkout.port.in.usecase.ClearCartUseCase;
import com.ecom.checkout.port.in.usecase.GetCartUseCase;
import com.ecom.checkout.port.in.usecase.RemoveCartItemUseCase;
import com.ecom.checkout.port.in.usecase.SetCartItemUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.ClearCartCmd;
import com.ecom.checkout.port.in.usecase.dto.command.GetCartQuery;
import com.ecom.checkout.port.in.usecase.dto.command.RemoveCartItemCmd;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Every call returns the whole cart. The optional {@code version} (in the body, or as a query parameter on the
 * DELETEs) is the version the caller last saw; a different current version answers 409. Nothing here checks who is
 * calling.
 */
@RestController
@RequestMapping("/carts/{customerId}")
@RequireRole({UserRole.ROLE_BUYER, UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class CartController {

   private final GetCartUseCase getCartUseCase;
   private final AddCartItemUseCase addCartItemUseCase;
   private final SetCartItemUseCase setCartItemUseCase;
   private final RemoveCartItemUseCase removeCartItemUseCase;
   private final ClearCartUseCase clearCartUseCase;

   @GetMapping
   public ResponseEntity<CartResponse> get(@PathVariable UUID customerId) {
      return ResponseEntity.ok(CartResponse.from(getCartUseCase.execute(new GetCartQuery(customerId))));
   }

   @PostMapping("/items")
   public ResponseEntity<CartResponse> add(@PathVariable UUID customerId,
                                           @Valid @RequestBody AddCartItemRequest request) {
      return ResponseEntity.ok(CartResponse.from(addCartItemUseCase.execute(request.toCommand(customerId))));
   }

   @PutMapping("/items/{skuId}")
   public ResponseEntity<CartResponse> set(@PathVariable UUID customerId,
                                           @PathVariable Long skuId,
                                           @Valid @RequestBody SetCartItemRequest request) {
      return ResponseEntity.ok(CartResponse.from(setCartItemUseCase.execute(request.toCommand(customerId, skuId))));
   }

   @DeleteMapping("/items/{skuId}")
   public ResponseEntity<CartResponse> remove(@PathVariable UUID customerId,
                                              @PathVariable Long skuId,
                                              @RequestParam(required = false) Long version) {
      return ResponseEntity.ok(CartResponse.from(
            removeCartItemUseCase.execute(new RemoveCartItemCmd(customerId, skuId, version))));
   }

   @DeleteMapping
   public ResponseEntity<CartResponse> clear(@PathVariable UUID customerId,
                                             @RequestParam(required = false) Long version) {
      return ResponseEntity.ok(CartResponse.from(clearCartUseCase.execute(new ClearCartCmd(customerId, version))));
   }
}
