package com.ecom.listing.adapter.in.web.controller.product;

import com.ecom.listing.adapter.in.web.controller.product.dto.request.CreateProductRequest;
import com.ecom.listing.adapter.in.web.controller.product.dto.request.UpdateProductRequest;
import com.ecom.listing.adapter.in.web.controller.product.dto.response.ProductResponse;
import com.ecom.listing.adapter.in.web.controller.product.dto.response.ProductPageResponse;
import com.ecom.listing.port.in.usecase.product.CreateProductUseCase;
import com.ecom.listing.port.in.usecase.product.DeleteProductUseCase;
import com.ecom.listing.port.in.usecase.product.GetProductUseCase;
import com.ecom.listing.port.in.usecase.product.ListProductsUseCase;
import com.ecom.listing.port.in.usecase.product.UpdateProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.DeleteProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.query.GetProductQuery;
import com.ecom.listing.port.in.usecase.product.dto.query.ListProductsQuery;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.security.Caller;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
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
   private final GetProductUseCase getProductUseCase;
   private final ListProductsUseCase listProductsUseCase;

   @GetMapping("/{id}")
   public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
      return ResponseEntity.ok(ProductResponse.from(getProductUseCase.execute(new GetProductQuery(id))));
   }

   /** Anyone can list; the query string is bound straight into {@link ListProductsQuery}. */
   @GetMapping
   public ResponseEntity<ProductPageResponse> listProducts(@Valid ListProductsQuery query) {
      return ResponseEntity.ok(ProductPageResponse.from(listProductsUseCase.execute(query)));
   }

   /** Sellers only: the product belongs to the seller in the access token. */
   @PostMapping
   @RequireRole(UserRole.ROLE_SELLER)
   public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
      ProductResult result = createProductUseCase.execute(request.toCommand(Caller.sellerId()));
      ProductResponse response = ProductResponse.from(result);
      return ResponseEntity.created(URI.create("/products/" + response.id())).body(response);
   }

   @PatchMapping("/{id}")
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<ProductResponse> updateProduct(
         @PathVariable UUID id,
         @Valid @RequestBody UpdateProductRequest request
   ) {
      ProductResult result = updateProductUseCase.execute(request.toCommand(id));
      return ResponseEntity.ok(ProductResponse.from(result));
   }

   @PutMapping("/{id}")
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<ProductResponse> putProduct(
         @PathVariable UUID id,
         @Valid @RequestBody UpdateProductRequest request
   ) {
      ProductResult result = updateProductUseCase.execute(request.toCommand(id));
      return ResponseEntity.ok(ProductResponse.from(result));
   }

   @DeleteMapping("/{id}")
   @RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
   public ResponseEntity<ProductResponse> deleteProduct(
         @PathVariable UUID id,
         @RequestParam long version
   ) {
      ProductResult result = deleteProductUseCase.execute(new DeleteProductCmd(id, version));
      return ResponseEntity.ok(ProductResponse.from(result));
   }
}
