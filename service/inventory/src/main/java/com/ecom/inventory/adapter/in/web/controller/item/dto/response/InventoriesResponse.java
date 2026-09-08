package com.ecom.inventory.adapter.in.web.controller.item.dto.response;

import com.ecom.inventory.port.in.usecase.item.dto.result.InventoriesResult;

import java.util.List;

public record InventoriesResponse(List<InventoryResponse> items) {

   public static InventoriesResponse from(InventoriesResult result) {
      return new InventoriesResponse(result.items().stream().map(InventoryResponse::from).toList());
   }
}
