package com.ecom.listing.port.out.persistence.product;

import com.ecom.listing.domain.entity.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadProductPort {

   Optional<Product> load(UUID id);

   List<Product> load(Iterable<UUID> ids);
}
