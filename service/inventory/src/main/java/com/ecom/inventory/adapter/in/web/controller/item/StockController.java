package com.ecom.inventory.adapter.in.web.controller.item;

import com.ecom.inventory.adapter.in.web.controller.item.dto.request.AddStockRequest;
import com.ecom.inventory.adapter.in.web.controller.item.dto.request.AdjustStockRequest;
import com.ecom.inventory.adapter.in.web.controller.item.dto.request.RemoveStockRequest;
import com.ecom.inventory.adapter.in.web.controller.item.dto.response.InventoryResponse;
import com.ecom.inventory.port.in.usecase.item.AddStockUseCase;
import com.ecom.inventory.port.in.usecase.item.AdjustStockUseCase;
import com.ecom.inventory.port.in.usecase.item.RemoveStockUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/inventory/{skuId}/stock")
@RequiredArgsConstructor
public class StockController {

   private final AdjustStockUseCase adjustStockUseCase;
   private final AddStockUseCase addStockUseCase;
   private final RemoveStockUseCase removeStockUseCase;

   @PostMapping
   public ResponseEntity<InventoryResponse> adjustStock(
         @PathVariable Long skuId,
         @Valid @RequestBody AdjustStockRequest request
   ) {
      InventoryResult result = adjustStockUseCase.execute(request.toCommand(skuId));
      return ResponseEntity.ok(InventoryResponse.from(result));
   }

   @PostMapping("/add")
   public ResponseEntity<InventoryResponse> addStock(
         @PathVariable Long skuId,
         @Valid @RequestBody AddStockRequest request
   ) {
      InventoryResult result = addStockUseCase.execute(request.toCommand(skuId));
      return ResponseEntity.ok(InventoryResponse.from(result));
   }

   @PostMapping("/remove")
   public ResponseEntity<InventoryResponse> removeStock(
         @PathVariable Long skuId,
         @Valid @RequestBody RemoveStockRequest request
   ) {
      InventoryResult result = removeStockUseCase.execute(request.toCommand(skuId));
      return ResponseEntity.ok(InventoryResponse.from(result));
   }
}
