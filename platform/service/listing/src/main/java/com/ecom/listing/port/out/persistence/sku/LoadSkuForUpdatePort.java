package com.ecom.listing.port.out.persistence.sku;

import com.ecom.listing.domain.entity.Sku;

import java.util.Optional;
import java.util.UUID;

public interface LoadSkuForUpdatePort {

   Optional<Sku> loadForUpdate(UUID productId, Long skuId);
}
