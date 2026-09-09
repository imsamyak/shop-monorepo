package com.ecom.catalog.service;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.contract.inventory.Inventory;
import com.ecom.contract.product.Product;
import com.ecom.contract.product.Sku;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogProjectionTest {

   private static final Instant T0 = Instant.parse("2026-09-01T10:00:00Z");

   private final InMemoryCatalogStore store = new InMemoryCatalogStore();
   private Instant now = T0;
   private CatalogProjection projection;

   @BeforeEach
   void setUp() {
      Clock clock = new Clock() {
         public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
         public Clock withZone(java.time.ZoneId zone) { return this; }
         public Instant instant() { return now; }
      };
      projection = new CatalogProjection(store, store, store, store, clock);
   }

   private CatalogProduct product(String id) {
      return store.products.get(id);
   }

   private static Product.Created created(String id, long version) {
      return new Product.Created(id, "seller-1", "Phone " + version, "A phone", new BigDecimal("500.00"), version);
   }

   private static Sku.Created skuCreated(String skuId, String productId, String price, long version) {
      return new Sku.Created(skuId, productId, "seller-1", new BigDecimal(price), Map.of("color", "black"), version);
   }

   private void stock(String skuId, int available, long version) {
      projection.apply(new Inventory.StockAdded(skuId, available, available, available, version));
   }

   @Nested
   class ProductEvents {

      @Test
      void aCreatedProductIsVisibleAndRemembersWhenTheCatalogFirstSawIt() {
         projection.apply(created("p1", 1));

         CatalogProduct p = product("p1");
         assertThat(p.visible()).isTrue();
         assertThat(p.sellerId()).isEqualTo("seller-1");
         assertThat(p.title()).isEqualTo("Phone 1");
         assertThat(p.price()).isEqualByComparingTo("500.00");
         assertThat(p.createdAt()).isEqualTo(T0);
         assertThat(p.availability()).isEqualTo(Availability.OUT_OF_STOCK);
      }

      @Test
      void aDuplicateDeliveryChangesNothingAndNeverMovesCreatedAt() {
         projection.apply(created("p1", 1));
         int saves = store.productSaves;
         now = T0.plusSeconds(60);

         projection.apply(created("p1", 1));

         assertThat(product("p1").createdAt()).isEqualTo(T0);
         assertThat(store.productSaves).isEqualTo(saves);
      }

      @Test
      void aNewerUpdateIsAppliedAndAnOlderOrRepeatedOneIgnored() {
         projection.apply(created("p1", 1));
         projection.apply(new Product.Updated("p1", "Phone v3", "newer", 3));
         projection.apply(new Product.Updated("p1", "Phone v2", "late", 2));
         projection.apply(new Product.Updated("p1", "Phone v3 again", "same version", 3));

         assertThat(product("p1").title()).isEqualTo("Phone v3");
         assertThat(product("p1").description()).isEqualTo("newer");
         assertThat(product("p1").version()).isEqualTo(3);
      }

      @Test
      void anUpdateThatArrivesBeforeTheCreationKeepsItsTextButTheCreationStillSuppliesTheRest() {
         projection.apply(new Product.Updated("p1", "Phone v2", "updated", 2));
         assertThat(product("p1").visible()).as("not shown before Created").isFalse();

         now = T0.plusSeconds(5);
         projection.apply(created("p1", 1));

         CatalogProduct p = product("p1");
         assertThat(p.visible()).isTrue();
         assertThat(p.title()).isEqualTo("Phone v2");
         assertThat(p.sellerId()).isEqualTo("seller-1");
         assertThat(p.price()).isEqualByComparingTo("500.00");
         assertThat(p.createdAt()).isEqualTo(T0.plusSeconds(5));
      }

      @Test
      void aDeletionIsFinalWhateverItsVersionAndNoLateEventBringsTheProductBack() {
         projection.apply(created("p1", 1));
         projection.apply(new Product.Updated("p1", "Phone v5", "x", 5));

         projection.apply(new Product.Deleted("p1", 2));
         projection.apply(new Product.Updated("p1", "Phone v9", "late", 9));
         projection.apply(created("p1", 10));

         CatalogProduct p = product("p1");
         assertThat(p.deleted()).isTrue();
         assertThat(p.visible()).isFalse();
         assertThat(p.title()).isEqualTo("Phone v5");
      }

      @Test
      void aDeletionBeforeTheCreationLeavesATombstoneThatKeepsTheProductHidden() {
         projection.apply(new Product.Deleted("p1", 3));
         projection.apply(created("p1", 1));

         assertThat(product("p1").visible()).isFalse();
         assertThat(product("p1").createdAt()).isNull();
      }
   }

   @Nested
   class SkusAndAvailability {

      @Test
      void aProductWithoutSkusIsOutOfStock() {
         projection.apply(created("p1", 1));

         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);
         assertThat(product("p1").skus()).isEmpty();
         assertThat(product("p1").lowestSkuPrice()).isNull();
      }

      @Test
      void aSkuWithoutStockIsOutOfStockAndStockMakesTheProductAvailable() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);
         assertThat(product("p1").skus()).hasSize(1);

         stock("s1", 7, 1);

         CatalogProduct p = product("p1");
         assertThat(p.availability()).isEqualTo(Availability.AVAILABLE);
         assertThat(p.skus().get(0).availableStock()).isEqualTo(7);
         assertThat(p.skus().get(0).properties()).containsEntry("color", "black");
      }

      @Test
      void aProductIsAvailableWhenAtLeastOneSkuHasStock() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         projection.apply(skuCreated("s2", "p1", "420.00", 1));
         stock("s1", 3, 1);
         stock("s2", 0, 1);
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);

         projection.apply(new Inventory.AvailabilityChanged("s1", 3, 3, 0, 2));
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);

         projection.apply(new Inventory.StockAdjusted("s2", 5, 5, 5, 2));
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);
      }

      @Test
      void aReleasedReservationMakesTheProductAvailableAgain() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         stock("s1", 2, 1);

         projection.apply(new Inventory.AvailabilityChanged("s1", 2, 2, 0, 2));
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);

         projection.apply(new Inventory.AvailabilityChanged("s1", 2, 0, 2, 3));
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);
      }

      @Test
      void anOlderStockSnapshotNeverOverwritesANewerOne() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         projection.apply(new Inventory.AvailabilityChanged("s1", 5, 5, 0, 3));

         projection.apply(new Inventory.StockAdded("s1", 5, 5, 5, 2));
         projection.apply(new Inventory.AvailabilityChanged("s1", 5, 5, 0, 3));

         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);
         assertThat(store.skus.get("s1").availableStock()).isZero();
      }

      @Test
      void theLowestSkuPriceIsKeptNextToTheDefaultPriceAndFollowsSkuUpdates() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         projection.apply(skuCreated("s2", "p1", "420.00", 1));
         assertThat(product("p1").price()).isEqualByComparingTo("500.00");
         assertThat(product("p1").lowestSkuPrice()).isEqualByComparingTo("420.00");
         assertThat(product("p1").listPrice()).isEqualByComparingTo("420.00");

         projection.apply(new Sku.Updated("s2", "p1", new BigDecimal("480.00"), Map.of("color", "red"), 2));
         assertThat(product("p1").lowestSkuPrice()).isEqualByComparingTo("450.00");
         assertThat(product("p1").skus()).extracting(s -> s.properties().get("color")).containsExactly("black", "red");
      }

      @Test
      void anOlderSkuUpdateIsIgnored() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         projection.apply(new Sku.Updated("s1", "p1", new BigDecimal("400.00"), Map.of(), 3));

         projection.apply(new Sku.Updated("s1", "p1", new BigDecimal("999.00"), Map.of(), 2));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));

         assertThat(product("p1").lowestSkuPrice()).isEqualByComparingTo("400.00");
      }

      @Test
      void aDeletedSkuNoLongerCountsAndALateEventCannotBringItBack() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         stock("s1", 4, 1);
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);

         projection.apply(new Sku.Deleted("s1", "p1", 1));
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);
         assertThat(product("p1").skus()).isEmpty();

         projection.apply(new Sku.Updated("s1", "p1", new BigDecimal("450.00"), Map.of(), 8));
         stock("s1", 9, 7);
         assertThat(product("p1").skus()).isEmpty();
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);
      }

      @Test
      void aDeletedSkuDoesNotHideTheStockOfTheOthers() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         projection.apply(skuCreated("s2", "p1", "420.00", 1));
         stock("s1", 4, 1);
         stock("s2", 1, 1);

         projection.apply(new Sku.Deleted("s1", "p1", 2));

         assertThat(product("p1").skus()).extracting(s -> s.skuId()).containsExactly("s2");
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);
         assertThat(product("p1").lowestSkuPrice()).isEqualByComparingTo("420.00");
      }

      @Test
      void aSkuDeletedBeforeItsCreationStaysDeleted() {
         projection.apply(created("p1", 1));
         projection.apply(new Sku.Deleted("s1", "p1", 4));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));

         assertThat(product("p1").skus()).isEmpty();
      }
   }

   @Nested
   class EventsInTheWrongOrder {

      @Test
      void aSkuBeforeItsProductIsStoredAndShownOnceTheProductArrives() {
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         stock("s1", 2, 1);

         assertThat(product("p1").visible()).as("the product itself is still unknown").isFalse();
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);

         projection.apply(created("p1", 1));

         CatalogProduct p = product("p1");
         assertThat(p.visible()).isTrue();
         assertThat(p.skus()).hasSize(1);
         assertThat(p.availability()).isEqualTo(Availability.AVAILABLE);
         assertThat(p.lowestSkuPrice()).isEqualByComparingTo("450.00");
      }

      @Test
      void stockBeforeItsSkuIsKeptAndAppliedWhenTheSkuArrives() {
         stock("s1", 6, 4);
         assertThat(store.products).as("no product is known for the SKU yet").isEmpty();

         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));

         assertThat(product("p1").skus().get(0).availableStock()).isEqualTo(6);
         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);
      }

      @Test
      void everyOrderOfTheSameEventsEndsInTheSameProduct() {
         Runnable[] events = {
               () -> projection.apply(created("p1", 1)),
               () -> projection.apply(new Product.Updated("p1", "Phone v2", "two", 2)),
               () -> projection.apply(skuCreated("s1", "p1", "450.00", 1)),
               () -> projection.apply(new Sku.Updated("s1", "p1", new BigDecimal("430.00"), Map.of("color", "red"), 2)),
               () -> stock("s1", 5, 1),
               () -> projection.apply(new Inventory.AvailabilityChanged("s1", 5, 5, 0, 2)),
               () -> projection.apply(new Inventory.StockAdjusted("s1", 3, 8, 8, 3)),
         };
         int[][] orders = {{0, 1, 2, 3, 4, 5, 6}, {6, 5, 4, 3, 2, 1, 0}, {3, 6, 0, 4, 1, 5, 2}, {2, 4, 6, 0, 3, 5, 1}};
         CatalogProduct expected = null;
         for (int[] order : orders) {
            InMemoryCatalogStore fresh = new InMemoryCatalogStore();
            projection = new CatalogProjection(fresh, fresh, fresh, fresh, Clock.fixed(T0, ZoneOffset.UTC));
            for (int index : order) {
               events[index].run();
               events[index].run();
            }
            CatalogProduct result = fresh.products.get("p1").toBuilder().revision(0).build();
            if (expected == null) {
               expected = result;
            }
            assertThat(result).isEqualTo(expected);
         }
         assertThat(expected.title()).isEqualTo("Phone v2");
         assertThat(expected.availability()).isEqualTo(Availability.AVAILABLE);
         assertThat(expected.lowestSkuPrice()).isEqualByComparingTo("430.00");
         assertThat(expected.skus().get(0).availableStock()).isEqualTo(8);
      }
   }

   @Nested
   class ConcurrentWriters {

      @Test
      void aSaveThatLosesTheRaceIsRepeatedFromTheFreshlyLoadedState() {
         store.conflictOnNextSaves(2);

         projection.apply(created("p1", 1));

         assertThat(product("p1").visible()).isTrue();
      }

      @Test
      void aWriterThatNeverWinsGivesUpSoTheBrokerRetriesTheMessage() {
         store.conflictOnNextSaves(Integer.MAX_VALUE);

         assertThatThrownBy(() -> projection.apply(created("p1", 1)))
               .isInstanceOf(IllegalStateException.class)
               .hasMessageContaining("p1");
      }

      @Test
      void aRepeatedSkuEventStillRefreshesTheProductAfterADeliveryThatFailedHalfway() {
         projection.apply(created("p1", 1));
         projection.apply(skuCreated("s1", "p1", "450.00", 1));
         // the SKU was saved but the product refresh never happened (the first delivery crashed in between)
         store.skus.put("s1", store.skus.get("s1").withStock(3, 1).toBuilder().revision(5).build());
         assertThat(product("p1").availability()).isEqualTo(Availability.OUT_OF_STOCK);

         stock("s1", 3, 1);

         assertThat(product("p1").availability()).isEqualTo(Availability.AVAILABLE);
      }
   }
}
