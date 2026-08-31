package com.ecom.listing.port.out.persistence.product;

import com.ecom.listing.domain.entity.Product;

public interface SaveProductPort {

   Product save(Product product);
}
