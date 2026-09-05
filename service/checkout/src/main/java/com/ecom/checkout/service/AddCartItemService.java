package com.ecom.checkout.service;

import com.ecom.checkout.port.in.usecase.AddCartItemUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.AddCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Adds to the quantity already in the cart for the SKU, or starts a line for it.
 */
@Service
@Validated
@RequiredArgsConstructor
public class AddCartItemService implements AddCartItemUseCase {

   private final CartEditor cartEditor;

   @Override
   public CartResult execute(AddCartItemCmd cmd) {
      return cartEditor.edit(cmd.customerId(), cmd.expectedVersion(), cart -> cart.add(cmd.skuId(), cmd.quantity()));
   }
}
