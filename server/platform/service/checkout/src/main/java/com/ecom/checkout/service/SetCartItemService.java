package com.ecom.checkout.service;

import com.ecom.checkout.port.in.usecase.SetCartItemUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.SetCartItemCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Sets the quantity of the SKU, starting a line for it if the cart has none.
 */
@Service
@Validated
@RequiredArgsConstructor
public class SetCartItemService implements SetCartItemUseCase {

   private final CartEditor cartEditor;

   @Override
   public CartResult execute(SetCartItemCmd cmd) {
      return cartEditor.edit(cmd.customerId(), cmd.expectedVersion(), cart -> cart.set(cmd.skuId(), cmd.quantity()));
   }
}
