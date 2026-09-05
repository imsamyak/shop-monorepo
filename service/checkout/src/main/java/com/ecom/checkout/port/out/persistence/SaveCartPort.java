package com.ecom.checkout.port.out.persistence;

import com.ecom.checkout.domain.entity.Cart;

public interface SaveCartPort {

   Cart save(Cart cart);
}
