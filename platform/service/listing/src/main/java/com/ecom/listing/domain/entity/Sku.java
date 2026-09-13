package com.ecom.listing.domain.entity;

import com.ecom.listing.domain.converter.SkuPropertyMapConverter;
import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * A purchasable variant of a product. Soft-deleted rows are hidden by {@link SQLRestriction}. The unique constraint
 * covers {@code deletion_token}, which is 0 for live rows and the SKU id once deleted, so the same property
 * combination can be re-created after a SKU was deleted while two live SKUs can never share it.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@SQLRestriction("is_deleted = false")
@Table(
    name = "product_skus",
    check = {
        @CheckConstraint(constraint = "price >= 0", name = "chk_product_skus_price")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"product_id", "sku_properties", "deletion_token"},
            name = "uc_product_skus_product_properties")
    }
)
public class Sku extends DomainEntity {

   @Convert(converter = SkuPropertyMapConverter.class)
   @Column(name = "sku_properties", nullable = false, length = 2048)
   private TreeMap<String, String> properties = new TreeMap<>();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "seller_id", nullable = false, updatable = false)
   private UUID sellerId;

   @ManyToOne(fetch = FetchType.LAZY, optional = false)
   @JoinColumn(name = "product_id", nullable = false, updatable = false)
   private Product product;

   @Column(name = "price", nullable = false)
   private BigDecimal price;

   @Column(name = "deletion_token", nullable = false)
   private long deletionToken = 0;

   @Builder
   public static Sku of(Product product, BigDecimal price) {
      if (product == null) {
         throw BusinessRuleException.of("A SKU must belong to a product");
      }
      requireValidPrice(price);
      return new Sku(new TreeMap<>(), null, product.getSellerId(), product, price, 0);
   }

   public Sku setPrice(BigDecimal price) {
      requireValidPrice(price);
      this.price = price;
      return this;
   }

   /**
    * Sets (or, with a null value, removes) one property. The map is replaced rather than mutated so Hibernate always
    * sees the change, whatever mutability it assumes for the converted attribute.
    */
   public Sku setProperty(String name, String value) {

      if (name == null || name.isBlank()) {
         return this;
      }

      name = name.strip();

      if (value != null && value.isBlank()) {
         return this;
      }

      TreeMap<String, String> updated = new TreeMap<>(this.properties);
      if (value == null) {
         updated.remove(name);
      } else {
         updated.put(name, value.strip());
      }
      this.properties = updated;
      return this;
   }

   public Sku removeProperty(String name) {
      return setProperty(name, null);
   }

   public Sku setProperties(Map<String, String> properties) {
      if (properties != null) {
         properties.forEach(this::setProperty);
      }
      return this;
   }

   /** Soft-deletes this SKU and frees its property combination for reuse. */
   public boolean softDelete() {
      boolean changed = markAsDeleted();
      if (changed) {
         this.deletionToken = this.id != null ? this.id : 0;
      }
      return changed;
   }

   private static void requireValidPrice(BigDecimal price) {
      if (price == null || price.signum() < 0) {
         throw BusinessRuleException.of("SKU price must be provided and cannot be negative");
      }
   }

   @Override
   public String getEntityName() {
      return "SKU";
   }
}
