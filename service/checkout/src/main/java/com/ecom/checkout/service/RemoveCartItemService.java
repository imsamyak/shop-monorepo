package com.ecom.checkout.service;

import com.ecom.checkout.port.in.usecase.RemoveCartItemUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.RemoveCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Removing a SKU that is not in the cart is not an error: the cart comes back unchanged.
 */
@Service
@Validated
@RequiredArgsConstructor
public class RemoveCartItemService implements RemoveCartItemUseCase {

   private final CartEditor cartEditor;

   @Override
   public CartResult execute(RemoveCartItemCmd cmd) {
      return cartEditor.edit(cmd.customerId(), cmd.expectedVersion(), cart -> cart.remove(cmd.skuId()));
   }
}
