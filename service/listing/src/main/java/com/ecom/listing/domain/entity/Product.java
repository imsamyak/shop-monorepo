package com.ecom.listing.domain.entity;

import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A product owned by one seller. Deleted products are soft-deleted ({@code is_deleted = true}) and are invisible to
 * every query through {@link SQLRestriction}.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@SQLRestriction("is_deleted = false")
@Table(
    name = "products",
    check = {
        @CheckConstraint(
            constraint = "char_length(title) >= 5 AND char_length(title) <= 50",
            name = "chk_products_title_len"
        ),
        @CheckConstraint(
            constraint = "description IS NULL OR char_length(description) <= 200",
            name = "chk_products_description_len"
        ),
        @CheckConstraint(
            constraint = "default_price IS NULL OR default_price >= 1",
            name = "chk_products_default_price"
        )
    }
)
public class Product extends DomainEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(name = "seller_id", nullable = false, updatable = false)
   private UUID sellerId;

   @Column(name = "title", nullable = false)
   private String title;

   @Column(length = 200)
   private String description;

   @DecimalMin(value = "1.00", inclusive = true, message = "Price must be at least 1.00 if provided")
   @Column(name = "default_price")
   private BigDecimal defaultPrice;

   /**
    * The pictures of the product in display order (the first is the main one), as addresses.
    */
   @ElementCollection(fetch = FetchType.EAGER)
   @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
   @OrderColumn(name = "position")
   @Column(name = "url", nullable = false, length = 500)
   private List<String> images = new ArrayList<>();

   public static Product of(UUID sellerId, String title, String description, BigDecimal defaultPrice) {
      return of(sellerId, title, description, defaultPrice, List.of());
   }

   @Builder
   public static Product of(UUID sellerId, String title, String description, BigDecimal defaultPrice, List<String> images) {
      if (sellerId == null) {
         throw BusinessRuleException.of("A product must belong to a seller");
      }
      Product product = new Product(null, sellerId, title, description, defaultPrice, new ArrayList<>());
      product.setImages(images);
      return product;
   }

   public Product setTitle(String title) {
      this.title = title;
      return this;
   }

   public Product setDescription(String description) {
      this.description = description;
      return this;
   }

   /**
    * Replaces the pictures; the order of the given list is the display order. {@code null} means none.
    */
   public Product setImages(List<String> images) {
      this.images.clear();
      if (images != null) {
         this.images.addAll(images);
      }
      return this;
   }

   public Product setDefaultPrice(BigDecimal defaultPrice) {
      this.defaultPrice = defaultPrice;
      return this;
   }

   public Product update(String title, String description) {
      if (title != null) {
         this.title = title;
      }
      if (description != null) {
         this.description = description;
      }
      return this;
   }

   @Override
   public String getEntityName() {
      return "PRODUCT";
   }
}
