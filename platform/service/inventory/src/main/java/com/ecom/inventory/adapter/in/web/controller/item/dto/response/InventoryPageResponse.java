package com.ecom.inventory.adapter.in.web.controller.item.dto.response;

import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryPageResult;

import java.util.List;

public record InventoryPageResponse(
      List<InventoryResponse> items,
      int page,
      int size,
      long totalElements,
      int totalPages
) {
   public static InventoryPageResponse from(InventoryPageResult result) {
      return new InventoryPageResponse(
            result.items().stream().map(InventoryResponse::from).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
      );
   }
}
