package com.ecom.checkout.port.out.persistence;

import com.ecom.checkout.domain.entity.Checkout;

public interface SaveCheckoutPort {

   Checkout save(Checkout checkout);
}
