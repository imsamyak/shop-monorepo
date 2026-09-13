package com.ecom.contract.product;

import com.ecom.shared.messaging.DomainEvent;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Domain events emitted by the Product boundary for Sku lifecycle mutations.
 * <p>
 * {@code version} is the SKU's version after the change that caused the event (see {@link Product}). {@code Deleted}
 * is final: a consumer applies it whatever version it carries. When deleting a product deletes its SKUs, their
 * {@code Deleted} events carry the product's version.
 */
public sealed interface Sku extends DomainEvent {

   String skuId();

   @Override
   default String aggregateId() {
      return skuId();
   }

   @Builder
   record Created(String skuId, String productId, String sellerId, BigDecimal price, Map<String, String> properties, long version) implements Sku {
   }

   @Builder
   record Updated(String skuId, String productId, BigDecimal price, Map<String, String> properties, long version) implements Sku {
   }

   @Builder
   record Deleted(String skuId, String productId, long version) implements Sku {
   }
}
