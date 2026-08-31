package com.ecom.listing.domain.entity;

import com.ecom.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductAndSkuTest {

   private final UUID seller = UUID.randomUUID();

   private Product product() {
      return Product.of(seller, "Laptop Pro", "desc", BigDecimal.valueOf(100));
   }

   @Test
   void aProductMustHaveASeller() {
      assertThatThrownBy(() -> Product.of(null, "Laptop Pro", "d", BigDecimal.TEN))
            .isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void aSkuInheritsTheSellerOfItsProduct() {
      Sku sku = Sku.of(product(), BigDecimal.TEN);

      assertThat(sku.getSellerId()).isEqualTo(seller);
      assertThat(sku.getProperties()).isEmpty();
   }

   @Test
   void aSkuNeedsAValidPrice() {
      assertThatThrownBy(() -> Sku.of(product(), null)).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Sku.of(product(), BigDecimal.valueOf(-1))).isInstanceOf(BusinessRuleException.class);
      assertThatThrownBy(() -> Sku.of(product(), BigDecimal.TEN).setPrice(BigDecimal.valueOf(-5)))
            .isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void propertiesAreStrippedAndBlankValuesIgnored() {
      Sku sku = Sku.of(product(), BigDecimal.TEN);

      sku.setProperty(" color ", " red ").setProperty("size", "  ").setProperty(" ", "x");

      assertThat(sku.getProperties()).isEqualTo(Map.of("color", "red"));
   }

   @Test
   void aNullValueRemovesAProperty() {
      Sku sku = Sku.of(product(), BigDecimal.TEN).setProperty("color", "red");

      sku.removeProperty("color");

      assertThat(sku.getProperties()).isEmpty();
   }

   @Test
   void changingPropertiesReplacesTheMapInsteadOfMutatingIt() {
      Sku sku = Sku.of(product(), BigDecimal.TEN).setProperty("color", "red");
      var before = sku.getProperties();

      sku.setProperty("size", "XL");

      assertThat(sku.getProperties()).isNotSameAs(before);
      assertThat(before).isEqualTo(Map.of("color", "red"));
   }

   @Test
   void softDeleteMarksTheSkuOnlyOnce() {
      Sku sku = Sku.of(product(), BigDecimal.TEN);

      assertThat(sku.softDelete()).isTrue();
      assertThat(sku.isDeleted()).isTrue();
      assertThat(sku.softDelete()).isFalse();
   }
}
