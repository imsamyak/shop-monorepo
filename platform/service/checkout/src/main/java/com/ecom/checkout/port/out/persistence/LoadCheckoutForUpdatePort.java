package com.ecom.checkout.port.out.persistence;

import com.ecom.checkout.domain.entity.Checkout;

import java.util.Optional;
import java.util.UUID;

public interface LoadCheckoutForUpdatePort {

   Optional<Checkout> loadByIdForUpdate(UUID checkoutId);
}
