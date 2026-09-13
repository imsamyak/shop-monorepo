package com.ecom.checkout.port.out.persistence;

import com.ecom.checkout.domain.entity.Checkout;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadCheckoutPort {

   Optional<Checkout> loadById(UUID checkoutId);

   /** Ids of drafts whose expiry passed before {@code now}, oldest first, at most {@code limit}. */
   List<UUID> findExpiredDraftIds(Instant now, int limit);
}
