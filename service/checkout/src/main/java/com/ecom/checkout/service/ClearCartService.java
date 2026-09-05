package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.port.in.usecase.ClearCartUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.ClearCartCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Empties the cart; the cart itself stays.
 */
@Service
@Validated
@RequiredArgsConstructor
public class ClearCartService implements ClearCartUseCase {

   private final CartEditor cartEditor;

   @Override
   public CartResult execute(ClearCartCmd cmd) {
      return cartEditor.edit(cmd.customerId(), cmd.expectedVersion(), Cart::clear);
   }
}
