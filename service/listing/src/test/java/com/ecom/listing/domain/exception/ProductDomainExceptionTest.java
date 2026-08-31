package com.ecom.listing.domain.exception;

import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.domain.exception.sku.DuplicateSkuPropertiesException;
import com.ecom.listing.domain.exception.sku.SkuNotFoundException;
import com.ecom.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDomainExceptionTest {

   @Test
   void shouldCreateProductNotFoundExceptionUsingStaticMethod() {
      UUID id = UUID.randomUUID();
      ProductNotFoundException ex = ProductNotFoundException.of(id);

      assertThat(ex.getProductId()).isEqualTo(id);
      assertThat(ex.getIdentifier()).isEqualTo(id);
      assertThat(ex.getEntityName()).isEqualTo("Product");
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
   }

   @Test
   void shouldCreateSkuNotFoundExceptionUsingStaticMethod() {
      UUID productId = UUID.randomUUID();
      Long skuId = 999L;

      SkuNotFoundException ex = SkuNotFoundException.of(productId, skuId);
      assertThat(ex.getProductId()).isEqualTo(productId);
      assertThat(ex.getSkuId()).isEqualTo(skuId);
      assertThat(ex.getIdentifier()).isEqualTo(skuId);
      assertThat(ex.getEntityName()).isEqualTo("Sku");
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);

      SkuNotFoundException singleId = SkuNotFoundException.of(skuId);
      assertThat(singleId.getProductId()).isNull();
      assertThat(singleId.getSkuId()).isEqualTo(skuId);
   }

   @Test
   void shouldCreateDuplicateSkuPropertiesExceptionUsingStaticMethod() {
      UUID productId = UUID.randomUUID();
      Map<String, String> properties = Map.of("size", "XL", "color", "red");

      DuplicateSkuPropertiesException ex = DuplicateSkuPropertiesException.of(productId, properties);
      assertThat(ex.getProductId()).isEqualTo(productId);
      assertThat(ex.getProperties()).containsEntry("size", "XL").containsEntry("color", "red");
      assertThat(ex.getResourceName()).isEqualTo("Sku");
      assertThat(ex.getField()).isEqualTo("properties");
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
   }
}
