package com.ecom.listing.port.in.usecase.sku.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

public record SkuListResult(List<SkuResult> items) implements Result.Many<SkuResult> {

   public SkuListResult {
      items = items == null ? List.of() : List.copyOf(items);
   }
}
