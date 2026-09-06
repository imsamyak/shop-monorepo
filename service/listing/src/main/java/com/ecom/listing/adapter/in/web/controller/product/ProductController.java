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
import com.ecom.listing.port.in.usecase.product.dto.query.ProductSort;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.shared.exception.validation.InvalidCommandException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.util.Locale;
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

   /** {@code sort} is {@code field,direction} with field {@code createdAt} or {@code title}, e.g. {@code title,asc}. */
   @GetMapping
   public ResponseEntity<ProductPageResponse> listProducts(
         @RequestParam(required = false) UUID sellerId,
         @RequestParam(required = false) String title,
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size,
         @RequestParam(defaultValue = "createdAt,desc") String sort
   ) {
      ListProductsQuery query = new ListProductsQuery(sellerId, title, page, size, parseSort(sort));
      return ResponseEntity.ok(ProductPageResponse.from(listProductsUseCase.execute(query)));
   }

   private static ProductSort parseSort(String sort) {
      return switch (sort.strip().toLowerCase(Locale.ROOT)) {
         case "createdat,desc" -> ProductSort.CREATED_AT_DESC;
         case "createdat,asc", "createdat" -> ProductSort.CREATED_AT_ASC;
         case "title,asc", "title" -> ProductSort.TITLE_ASC;
         case "title,desc" -> ProductSort.TITLE_DESC;
         default -> throw InvalidCommandException.of(
            "Unsupported sort '" + sort + "'. Use createdAt or title, followed by ,asc or ,desc");
      };
   }

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
