package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.checkout.port.out.persistence.LoadCartPort;
import com.ecom.checkout.port.out.persistence.SaveCartPort;
import com.ecom.checkout.service.mapper.CartMapper;
import com.ecom.shared.validator.EntityVersionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The one transaction every cart edit goes through: load the customer's cart (a new one if there is none), check the
 * version the caller last saw, apply the change and save only if it changed something. A concurrent edit that slips
 * between the load and the save is rejected by the entity's version.
 */
@Component
@RequiredArgsConstructor
public class CartEditor {

   private final LoadCartPort loadCartPort;
   private final SaveCartPort saveCartPort;
   private final EntityVersionValidator versionValidator;
   private final CartMapper cartMapper;

   /**
    * @param change applies the edit to the cart and returns whether it changed anything
    */
   @Transactional
   public CartResult edit(UUID customerId, Long expectedVersion, Predicate<Cart> change) {
      Optional<Cart> stored = loadCartPort.loadByCustomerId(customerId);
      Cart cart = stored.orElseGet(() -> Cart.empty(customerId));
      if (expectedVersion != null) {
         versionValidator.validate(cart, expectedVersion);
      }
      if (!change.test(cart)) {
         return stored.isPresent() ? cartMapper.toResult(cart) : cartMapper.toEmptyResult(customerId);
      }
      return cartMapper.toResult(saveCartPort.save(cart));
   }
}
