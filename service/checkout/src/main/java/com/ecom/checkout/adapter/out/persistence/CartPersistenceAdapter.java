package com.ecom.checkout.adapter.out.persistence;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.port.out.persistence.LoadCartForUpdatePort;
import com.ecom.checkout.port.out.persistence.LoadCartPort;
import com.ecom.checkout.port.out.persistence.SaveCartPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartPersistenceAdapter implements
      LoadCartPort,
      LoadCartForUpdatePort,
      SaveCartPort {

   private final CartJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Cart> loadByCustomerId(UUID customerId) {
      return repository.findByCustomerId(customerId);
   }

   @Override
   @Transactional
   public Optional<Cart> loadByCustomerIdForUpdate(UUID customerId) {
      return repository.findByCustomerIdForUpdate(customerId);
   }

   @Override
   @Transactional
   public Cart save(Cart cart) {
      return repository.saveAndFlush(cart);
   }
}
