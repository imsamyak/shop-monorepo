package com.ecom.listing.adapter.in.web.controller.sku;

import com.ecom.listing.adapter.in.web.controller.sku.dto.request.CreateSkuRequest;
import com.ecom.listing.adapter.in.web.controller.sku.dto.request.UpdateSkuRequest;
import com.ecom.listing.adapter.in.web.controller.sku.dto.response.SkuResponse;
import com.ecom.listing.port.in.usecase.sku.CreateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.DeleteSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.UpdateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.DeleteSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/products/{productId}/skus")
@RequiredArgsConstructor
public class SkuController {

   private final CreateSkuUseCase createSkuUseCase;
   private final UpdateSkuUseCase updateSkuUseCase;
   private final DeleteSkuUseCase deleteSkuUseCase;

   @PostMapping
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
   public ResponseEntity<SkuResponse> updateSku(
         @PathVariable UUID productId,
         @PathVariable Long skuId,
         @Valid @RequestBody UpdateSkuRequest request
   ) {
      SkuResult result = updateSkuUseCase.execute(request.toCommand(productId, skuId));
      return ResponseEntity.ok(SkuResponse.from(result));
   }

   @DeleteMapping("/{skuId}")
   public ResponseEntity<SkuResponse> deleteSku(
         @PathVariable UUID productId,
         @PathVariable Long skuId,
         @RequestParam long version
   ) {
      SkuResult result = deleteSkuUseCase.execute(new DeleteSkuCmd(productId, skuId, version));
      return ResponseEntity.ok(SkuResponse.from(result));
   }
}
