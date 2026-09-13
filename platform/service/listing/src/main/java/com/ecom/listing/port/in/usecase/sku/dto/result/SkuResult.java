package com.ecom.listing.port.in.usecase.sku.dto.result;

import com.ecom.shared.dto.result.Result;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * @param version the version clients must send back when updating or deleting
 */
@Builder
public record SkuResult(
   Long skuId,
   UUID productId,
   UUID sellerId,
   BigDecimal price,
   Map<String, String> properties,
   long version
) implements Result.One {
}
