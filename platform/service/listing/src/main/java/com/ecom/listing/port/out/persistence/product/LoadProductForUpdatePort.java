package com.ecom.listing.port.out.persistence.product;

import com.ecom.listing.domain.entity.Product;

import java.util.Optional;
import java.util.UUID;

public interface LoadProductForUpdatePort {

   Optional<Product> loadForUpdate(UUID id);
}
