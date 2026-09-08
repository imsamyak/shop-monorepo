package com.ecom.inventory.adapter.in.web.controller.item;

import com.ecom.inventory.adapter.in.web.controller.item.dto.response.InventoriesResponse;
import com.ecom.inventory.adapter.in.web.controller.item.dto.response.InventoryPageResponse;
import com.ecom.inventory.adapter.in.web.controller.item.dto.response.InventoryResponse;
import com.ecom.inventory.port.in.usecase.item.GetInventoriesUseCase;
import com.ecom.inventory.port.in.usecase.item.GetInventoryUseCase;
import com.ecom.inventory.port.in.usecase.item.ListInventoryUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoriesQuery;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.query.ListInventoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Stock reads. The reservation reads live under {@code /inventory/reservations}; Spring prefers that literal path over
 * {@code /{skuId}}, so the two do not clash.
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

   private final GetInventoryUseCase getInventoryUseCase;
   private final GetInventoriesUseCase getInventoriesUseCase;
   private final ListInventoryUseCase listInventoryUseCase;

   @GetMapping("/{skuId}")
   public ResponseEntity<InventoryResponse> get(@PathVariable Long skuId) {
      return ResponseEntity.ok(InventoryResponse.from(getInventoryUseCase.execute(new GetInventoryQuery(skuId))));
   }

   /** With {@code skuIds} this is a bulk lookup, without it the paged listing sorted by sku id. */
   @GetMapping
   public ResponseEntity<?> list(
         @RequestParam(required = false) List<Long> skuIds,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size
   ) {
      if (skuIds != null) {
         return ResponseEntity.ok(InventoriesResponse.from(getInventoriesUseCase.execute(new GetInventoriesQuery(skuIds))));
      }
      return ResponseEntity.ok(InventoryPageResponse.from(listInventoryUseCase.execute(new ListInventoryQuery(page, size))));
   }
}
