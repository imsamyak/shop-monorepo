package com.ecom.checkout.port.out.persistence;

import com.ecom.checkout.domain.entity.Cart;

import java.util.Optional;
import java.util.UUID;

public interface LoadCartForUpdatePort {

   Optional<Cart> loadByCustomerIdForUpdate(UUID customerId);
}
