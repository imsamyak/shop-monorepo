package com.ecom.catalog.adapter.in.web;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.ProductSort;
import com.ecom.catalog.port.in.usecase.GetProductUseCase;
import com.ecom.catalog.port.in.usecase.ListProductsUseCase;
import com.ecom.catalog.port.in.usecase.dto.GetProductQuery;
import com.ecom.catalog.port.in.usecase.dto.ListProductsQuery;
import com.ecom.catalog.port.in.usecase.dto.ProductPageResult;
import com.ecom.catalog.port.in.usecase.dto.ProductResult;
import com.ecom.shared.exception.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    * @param sort         {@code createdAt,desc} (default, newest first), {@code createdAt,asc}, {@code price,asc} or {@code price,desc}
    * @param availability only {@code AVAILABLE} or only {@code OUT_OF_STOCK} products; both when absent
    */
   @GetMapping
   public ResponseEntity<ProductPageResult> list(
         @RequestParam(defaultValue = "0") int page,
         @RequestParam(defaultValue = "20") int size,
         @RequestParam(defaultValue = "createdAt,desc") String sort,
         @RequestParam(required = false) Availability availability
   ) {
      return ResponseEntity.ok(listProductsUseCase.execute(new ListProductsQuery(page, size, parse(sort), availability)));
   }

   @GetMapping("/{id}")
   public ResponseEntity<ProductResult> get(@PathVariable String id) {
      return ResponseEntity.ok(getProductUseCase.execute(new GetProductQuery(id)));
   }

   private static ProductSort parse(String sort) {
      try {
         return ProductSort.parse(sort);
      } catch (IllegalArgumentException e) {
         throw new ValidationException(e.getMessage());
      }
   }
}
