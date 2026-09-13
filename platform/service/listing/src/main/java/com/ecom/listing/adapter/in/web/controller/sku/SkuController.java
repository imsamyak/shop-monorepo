package com.ecom.listing.adapter.in.web.controller.sku;

import com.ecom.listing.adapter.in.web.controller.sku.dto.request.CreateSkuRequest;
import com.ecom.listing.adapter.in.web.controller.sku.dto.request.UpdateSkuRequest;
import com.ecom.listing.adapter.in.web.controller.sku.dto.response.SkuResponse;
import com.ecom.listing.port.in.usecase.sku.CreateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.DeleteSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.GetSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.ListSkusUseCase;
import com.ecom.listing.port.in.usecase.sku.UpdateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.DeleteSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.query.GetSkuQuery;
import com.ecom.listing.port.in.usecase.sku.dto.query.ListSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products/{productId}/skus")
@RequiredArgsConstructor
public class SkuController {

   private final CreateSkuUseCase createSkuUseCase;
   private final UpdateSkuUseCase updateSkuUseCase;
   private final DeleteSkuUseCase deleteSkuUseCase;
   private final GetSkuUseCase getSkuUseCase;
   private final ListSkusUseCase listSkusUseCase;

   @GetMapping
   public ResponseEntity<List<SkuResponse>> listSkus(@PathVariable UUID productId) {
      SkuListResult result = listSkusUseCase.execute(new ListSkusQuery(productId));
      return ResponseEntity.ok(result.items().stream().map(SkuResponse::from).toList());
   }

   @GetMapping("/{skuId}")
   public ResponseEntity<SkuResponse> getSku(@PathVariable UUID productId, @PathVariable Long skuId) {
      return ResponseEntity.ok(SkuResponse.from(getSkuUseCase.execute(new GetSkuQuery(productId, skuId))));
   }

   @PostMapping
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<SkuResponse> createSku(
         @PathVariable UUID productId,
         @Valid @RequestBody CreateSkuRequest request
   ) {
      SkuResult result = createSkuUseCase.execute(request.toCommand(productId));
      SkuResponse response = SkuResponse.from(result);
      return ResponseEntity.created(URI.create(String.format("/products/%s/skus/%d", productId, response.skuId())))
            .body(response);
   }

   @PutMapping("/{skuId}")
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<SkuResponse> updateSku(
         @PathVariable UUID productId,
         @PathVariable Long skuId,
         @Valid @RequestBody UpdateSkuRequest request
   ) {
      SkuResult result = updateSkuUseCase.execute(request.toCommand(productId, skuId));
      return ResponseEntity.ok(SkuResponse.from(result));
   }

   @DeleteMapping("/{skuId}")
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<SkuResponse> deleteSku(
         @PathVariable UUID productId,
         @PathVariable Long skuId,
         @RequestParam long version
   ) {
      SkuResult result = deleteSkuUseCase.execute(new DeleteSkuCmd(productId, skuId, version));
      return ResponseEntity.ok(SkuResponse.from(result));
   }
}
