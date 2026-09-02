package com.ecom.checkout.adapter.out.persistence;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.checkout.port.out.persistence.LoadCheckoutForUpdatePort;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import com.ecom.checkout.port.out.persistence.SaveCheckoutPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckoutPersistenceAdapter implements
      LoadCheckoutPort,
      LoadCheckoutForUpdatePort,
      SaveCheckoutPort {

   private final CheckoutJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Checkout> loadById(UUID checkoutId) {
      return repository.findById(checkoutId);
   }

   @Override
   @Transactional(readOnly = true)
   public List<UUID> findExpiredDraftIds(Instant now, int limit) {
      return repository.findIdsByStatusAndExpiresAtBefore(CheckoutStatus.DRAFT, now, PageRequest.of(0, limit));
   }

   @Override
   @Transactional
   public Optional<Checkout> loadByIdForUpdate(UUID checkoutId) {
      return repository.findByIdForUpdate(checkoutId);
   }

   @Override
   @Transactional
   public Checkout save(Checkout checkout) {
      return repository.saveAndFlush(checkout);
   }
}
