package com.ecom.listing.port.in.usecase.product.dto.result;

import com.ecom.shared.dto.result.Result;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @param version        the version clients must send back when updating or deleting
 * @param deletedSkuIds  only set by a delete: the SKUs that were removed together with the product
 */
@Builder
public record ProductResult(
   UUID id,
   UUID sellerId,
   String title,
   String description,
   BigDecimal defaultPrice,
   long version,
   List<Long> deletedSkuIds
) implements Result.One {

   public ProductResult {
      deletedSkuIds = deletedSkuIds == null ? List.of() : List.copyOf(deletedSkuIds);
   }

   public ProductResult withDeletedSkuIds(List<Long> skuIds) {
      return new ProductResult(id, sellerId, title, description, defaultPrice, version, skuIds);
   }
}
