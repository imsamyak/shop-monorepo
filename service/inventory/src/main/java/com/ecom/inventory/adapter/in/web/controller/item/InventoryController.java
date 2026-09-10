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
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Stock reads. The reservation reads live under {@code /inventory/reservations}; Spring prefers that literal path over
 * {@code /{skuId}}, so the two do not clash.
 */
@RestController
@RequestMapping("/inventory")
@RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN, UserRole.ROLE_SYSTEM})
@RequiredArgsConstructor
public class InventoryController {

   private final GetInventoryUseCase getInventoryUseCase;
   private final GetInventoriesUseCase getInventoriesUseCase;
   private final ListInventoryUseCase listInventoryUseCase;

   @GetMapping("/{skuId}")
   public ResponseEntity<InventoryResponse> get(@PathVariable Long skuId) {
      return ResponseEntity.ok(InventoryResponse.from(getInventoryUseCase.execute(new GetInventoryQuery(skuId))));
   }

   /**
    * With {@code skuIds} this is a bulk lookup, without it the paged listing sorted by sku id. The query string is
    * bound straight into {@link ListInventoryQuery}. Only admins and other services may look at stock in bulk.
    */
   @GetMapping
   @RequireRole({UserRole.ROLE_ADMIN, UserRole.ROLE_SYSTEM})
   public ResponseEntity<?> list(@Valid ListInventoryQuery query) {
      if (query.isLookup()) {
         return ResponseEntity.ok(InventoriesResponse.from(getInventoriesUseCase.execute(new GetInventoriesQuery(query.skuIds()))));
      }
      return ResponseEntity.ok(InventoryPageResponse.from(listInventoryUseCase.execute(query)));
   }
}
