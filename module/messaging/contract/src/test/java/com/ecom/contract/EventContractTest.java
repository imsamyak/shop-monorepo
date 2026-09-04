package com.ecom.contract;

import com.ecom.contract.checkout.Checkout;
import com.ecom.contract.inventory.Inventory;
import com.ecom.contract.inventory.Reservation;
import com.ecom.contract.product.Product;
import com.ecom.contract.product.Sku;
import com.ecom.shared.messaging.DomainEventRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventContractTest {

   private final DomainEventRegistry registry = new DomainEventRegistry();

   @Test
   @DisplayName("Resolves canonical signatures and aggregateId for product events")
   void productSignatures() {
      Product.Created created = new Product.Created("prod-1", "seller-1", "MacBook", "Pro M3", BigDecimal.valueOf(1999), 0L);
      Product.Updated updated = new Product.Updated("prod-1", "MacBook M3", "Updated desc", 1L);
      Sku.Created skuCreated = new Sku.Created("sku-1", "prod-1", "seller-1", BigDecimal.valueOf(1999), Map.of("color", "red"), 0L);
      Sku.Updated skuUpdated = new Sku.Updated("sku-1", "prod-1", BigDecimal.valueOf(1899), Map.of("color", "red"), 1L);

      Product.Deleted deleted = new Product.Deleted("prod-1", 2L);
      Sku.Deleted skuDeleted = new Sku.Deleted("sku-1", "prod-1", 2L);

      assertEquals("prod-1", created.aggregateId());
      assertEquals("prod-1", created.productId());
      assertEquals("prod-1", deleted.aggregateId());
      assertEquals("sku-1", skuCreated.aggregateId());
      assertEquals("sku-1", skuCreated.skuId());
      assertEquals("sku-1", skuDeleted.aggregateId());

      assertEquals("product.created", registry.getSignature(created).toString());
      assertEquals("product.updated", registry.getSignature(updated).toString());
      assertEquals("product.deleted", registry.getSignature(deleted).toString());
      assertEquals("sku.created", registry.getSignature(skuCreated).toString());
      assertEquals("sku.updated", registry.getSignature(skuUpdated).toString());
      assertEquals("sku.deleted", registry.getSignature(skuDeleted).toString());

      assertEquals(Product.Created.class, registry.getClazz("product.created"));
      assertEquals(Product.Updated.class, registry.getClazz("product.updated"));
      assertEquals(Product.Deleted.class, registry.getClazz("product.deleted"));
      assertEquals(Sku.Created.class, registry.getClazz("sku.created"));
      assertEquals(Sku.Updated.class, registry.getClazz("sku.updated"));
      assertEquals(Sku.Deleted.class, registry.getClazz("sku.deleted"));
      assertEquals(Product.Created.class, registry.getClazz("Product.Created"));
   }

   @Test
   @DisplayName("Resolves canonical signatures and aggregateId for inventory and reservation events")
   void inventorySignatures() {
      Inventory.StockAdded added = new Inventory.StockAdded("sku-1", 50, 150, 140, 3L);
      Inventory.StockAdjusted adjusted = new Inventory.StockAdjusted("sku-1", -10, 140, 130, 4L);
      Inventory.AvailabilityChanged availability = new Inventory.AvailabilityChanged("sku-1", 140, 10, 130, 5L);
      Reservation.Created reserved = new Reservation.Created("res-1", "chk-1", List.of(new Reservation.ReservedItem("sku-1", 2)), 0L);
      Reservation.Released released = new Reservation.Released("res-1", "Cancelled", 1L);

      assertEquals("sku-1", added.aggregateId());
      assertEquals("sku-1", added.skuId());
      assertEquals("res-1", reserved.aggregateId());
      assertEquals("res-1", reserved.reservationId());

      assertEquals("inventory.stock-added", registry.getSignature(added).toString());
      assertEquals("inventory.stock-adjusted", registry.getSignature(adjusted).toString());
      assertEquals("sku-1", availability.aggregateId());
      assertEquals("inventory.availability-changed", registry.getSignature(availability).toString());
      assertEquals(Inventory.AvailabilityChanged.class, registry.getClazz("inventory.availability-changed"));
      assertEquals("reservation.created", registry.getSignature(reserved).toString());
      assertEquals("reservation.released", registry.getSignature(released).toString());

      assertEquals(Inventory.StockAdded.class, registry.getClazz("inventory.stock-added"));
      assertEquals(Inventory.StockAdjusted.class, registry.getClazz("inventory.stock-adjusted"));
      assertEquals(Reservation.Created.class, registry.getClazz("reservation.created"));
      assertEquals(Reservation.Released.class, registry.getClazz("reservation.released"));
   }

   @Test
   @DisplayName("Resolves canonical signatures and aggregateId for checkout events")
   void checkoutSignatures() {
      Checkout.Drafted drafted = new Checkout.Drafted("chk-1", "usr-1", List.of(new Checkout.Item("sku-1", 2, BigDecimal.TEN, "seller-1")), 0L);
      Checkout.Committed committed = new Checkout.Committed("chk-1", "usr-1", List.of(new Checkout.Item("sku-1", 2, BigDecimal.TEN, "seller-1")), 1L);
      Checkout.Cancelled cancelled = new Checkout.Cancelled("chk-1", "Expired", 1L);

      assertEquals("chk-1", drafted.aggregateId());
      assertEquals("chk-1", drafted.checkoutId());
      assertEquals("chk-1", committed.aggregateId());
      assertEquals("chk-1", cancelled.aggregateId());

      assertEquals("checkout.drafted", registry.getSignature(drafted).toString());
      assertEquals("checkout.committed", registry.getSignature(committed).toString());
      assertEquals("checkout.cancelled", registry.getSignature(cancelled).toString());

      assertEquals(Checkout.Drafted.class, registry.getClazz("checkout.drafted"));
      assertEquals(Checkout.Committed.class, registry.getClazz("checkout.committed"));
      assertEquals(Checkout.Cancelled.class, registry.getClazz("checkout.cancelled"));
   }

   @Test
   @DisplayName("Carries the aggregate version in the payload")
   void versionIsPartOfThePayload() throws Exception {
      com.fasterxml.jackson.databind.ObjectMapper mapper = com.ecom.shared.json.JsonMappers.messaging();
      String json = mapper.writeValueAsString(new Inventory.StockAdded("sku-1", 5, 15, 12, 7L));

      com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
      assertEquals(7L, node.path("version").asLong());
      assertEquals(12, node.path("availableStock").asInt());
      assertEquals(0L, mapper.readValue("{\"productId\":\"p-1\"}", Product.Deleted.class).version());
   }
}
