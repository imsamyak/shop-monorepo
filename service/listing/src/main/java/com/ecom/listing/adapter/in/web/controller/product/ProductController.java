package com.ecom.listing.adapter.in.web.controller.product;

import com.ecom.listing.adapter.in.web.controller.product.dto.request.CreateProductRequest;
import com.ecom.listing.adapter.in.web.controller.product.dto.request.UpdateProductRequest;
import com.ecom.listing.adapter.in.web.controller.product.dto.response.ProductResponse;
import com.ecom.listing.port.in.usecase.product.CreateProductUseCase;
import com.ecom.listing.port.in.usecase.product.DeleteProductUseCase;
import com.ecom.listing.port.in.usecase.product.UpdateProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.DeleteProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

   private final CreateProductUseCase createProductUseCase;
   private final UpdateProductUseCase updateProductUseCase;
   private final DeleteProductUseCase deleteProductUseCase;

   @PostMapping
   public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
      ProductResult result = createProductUseCase.execute(request.toCommand());
      ProductResponse response = ProductResponse.from(result);
      return ResponseEntity.created(URI.create("/products/" + response.id())).body(response);
   }

   @PatchMapping("/{id}")
   public ResponseEntity<ProductResponse> updateProduct(
         @PathVariable UUID id,
         @Valid @RequestBody UpdateProductRequest request
   ) {
      ProductResult result = updateProductUseCase.execute(request.toCommand(id));
      return ResponseEntity.ok(ProductResponse.from(result));
   }

   @PutMapping("/{id}")
   public ResponseEntity<ProductResponse> putProduct(
         @PathVariable UUID id,
         @Valid @RequestBody UpdateProductRequest request
   ) {
      ProductResult result = updateProductUseCase.execute(request.toCommand(id));
      return ResponseEntity.ok(ProductResponse.from(result));
   }

   @DeleteMapping("/{id}")
   public ResponseEntity<ProductResponse> deleteProduct(
         @PathVariable UUID id,
         @RequestParam long version
   ) {
      ProductResult result = deleteProductUseCase.execute(new DeleteProductCmd(id, version));
      return ResponseEntity.ok(ProductResponse.from(result));
   }
}
