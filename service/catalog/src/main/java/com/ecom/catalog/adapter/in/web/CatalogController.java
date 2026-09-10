package com.ecom.catalog.adapter.in.web;

import com.ecom.catalog.port.in.usecase.GetProductUseCase;
import com.ecom.catalog.port.in.usecase.ListProductsUseCase;
import com.ecom.catalog.port.in.usecase.dto.GetProductQuery;
import com.ecom.catalog.port.in.usecase.dto.ListProductsQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductPageResult;
import com.ecom.catalog.port.in.usecase.dto.ProductResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The read-only API behind the home page.
 */
@RestController
@RequestMapping("/catalog/products")
@RequiredArgsConstructor
public class CatalogController {

   private final ListProductsUseCase listProductsUseCase;
   private final GetProductUseCase getProductUseCase;

   /**
    * The query string is bound straight into {@link ListProductsQuery}: {@code sort} is {@code createdAt,desc}
    * (default, newest first), {@code createdAt,asc}, {@code price,asc} or {@code price,desc}, and {@code availability}
    * limits the feed to {@code AVAILABLE} or {@code OUT_OF_STOCK} products.
    */
   @GetMapping
   public ResponseEntity<ProductPageResult> list(@Valid ListProductsQuery query) {
      return ResponseEntity.ok(listProductsUseCase.execute(query));
   }

   @GetMapping("/{id}")
   public ResponseEntity<ProductResult> get(@PathVariable String id) {
      return ResponseEntity.ok(getProductUseCase.execute(new GetProductQuery(id)));
   }
}
