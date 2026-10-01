package com.ecom.catalog;

import com.ecom.contract.inventory.Inventory;
import com.ecom.contract.product.Product;
import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxListener;
import com.ecom.shared.json.JsonMappers;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test of catalog against the real MongoDB from {@code bootstrap/local}: events go through the inbox
 * listener like production, and the home page is read over HTTP. Skipped when MongoDB is not running. Each run uses its
 * own database, which is dropped at the end.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CatalogIntegrationTest {

   private static final String DATABASE = "catalog_test_" + UUID.randomUUID().toString().replace("-", "");
   private static final DomainEventRegistry EVENTS = new DomainEventRegistry();

   @BeforeAll
   static void requireMongo() {
      boolean up;
      try (Socket socket = new Socket()) {
         socket.connect(new InetSocketAddress("127.0.0.1", 27017), 500);
         up = true;
      } catch (Exception e) {
         up = false;
      }
      Assumptions.assumeTrue(up, "MongoDB is not running; start the local resources with bootstrap/local/local.ps1 up");
   }

   @DynamicPropertySource
   static void database(DynamicPropertyRegistry registry) {
      registry.add("catalog.mongodb.database", () -> DATABASE);
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   InboxListener inboxListener;

   @Autowired
   MongoDatabase database;

   private final HttpClient http = HttpClient.newHttpClient();

   @AfterAll
   static void dropDatabase(@Autowired MongoDatabase database) {
      database.drop();
   }

   record Reply(int status, JsonNode body) {
   }

   private Reply get(String path) throws Exception {
      HttpResponse<String> response = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
      return new Reply(response.statusCode(), JsonMappers.messaging().readTree(response.body()));
   }

   private void deliver(DomainEvent event) throws Exception {
      inboxListener.listen("msg-" + UUID.randomUUID(), EVENTS.getSignature(event).toString(),
            JsonMappers.messaging().writeValueAsString(event));
   }

   private String newProduct(String title, String price) throws Exception {
      String id = UUID.randomUUID().toString();
      deliver(new Product.Created(id, "seller-1", title, "about " + title, new BigDecimal(price), List.of(), 1));
      Thread.sleep(5);
      return id;
   }

   private void newSku(String productId, String skuId, String price, int stock, long stockVersion) throws Exception {
      deliver(new Sku.Created(skuId, productId, "seller-1", new BigDecimal(price), Map.of("color", "black"), 1));
      if (stock > 0) {
         deliver(new Inventory.StockAdded(skuId, stock, stock, stock, stockVersion));
      }
   }

   private static List<String> ids(JsonNode page) {
      List<String> ids = new ArrayList<>();
      page.path("items").forEach(item -> ids.add(item.path("id").asText()));
      return ids;
   }

   @Test
   void theHomePageShowsNewestFirstAndFlipsBetweenAvailableAndOutOfStockWithTheInventoryEvents() throws Exception {
      String first = newProduct("First", "100.00");
      String second = newProduct("Second", "50.00");
      String sku = "sku-" + UUID.randomUUID();
      newSku(first, sku, "90.00", 0, 0);

      JsonNode feed = get("/api/catalog/products?size=100").body();
      assertThat(ids(feed)).containsSubsequence(second, first);
      assertThat(get("/api/catalog/products/" + first).body().path("availability").asText()).isEqualTo("OUT_OF_STOCK");

      deliver(new Inventory.StockAdded(sku, 5, 5, 5, 1));
      assertThat(get("/api/catalog/products/" + first).body().path("availability").asText()).isEqualTo("AVAILABLE");

      deliver(new Inventory.AvailabilityChanged(sku, 5, 5, 0, 2));
      assertThat(get("/api/catalog/products/" + first).body().path("availability").asText()).isEqualTo("OUT_OF_STOCK");

      deliver(new Inventory.AvailabilityChanged(sku, 5, 0, 5, 3));
      deliver(new Inventory.AvailabilityChanged(sku, 5, 5, 0, 2)); // late and older: ignored
      assertThat(get("/api/catalog/products/" + first).body().path("availability").asText()).isEqualTo("AVAILABLE");

      deliver(new Inventory.StockAdjusted(sku, -5, 0, 0, 4));
      assertThat(get("/api/catalog/products/" + first).body().path("availability").asText()).isEqualTo("OUT_OF_STOCK");
   }

   @Test
   void aProductShowsItsImagesInTheOrderTheSellerGaveThem() throws Exception {
      String id = UUID.randomUUID().toString();
      deliver(new Product.Created(id, "seller-1", "Phone", "d", new BigDecimal("5.00"),
         List.of("https://f.example.com/3.png", "https://f.example.com/1.png", "https://f.example.com/2.png"), 1));

      List<String> shown = new ArrayList<>();
      get("/api/catalog/products/" + id).body().path("images").forEach(node -> shown.add(node.asText()));
      assertThat(shown).containsExactly("https://f.example.com/3.png", "https://f.example.com/1.png", "https://f.example.com/2.png");

      deliver(new Product.Updated(id, "Phone", "d", List.of("https://f.example.com/2.png"), 2));
      assertThat(get("/api/catalog/products/" + id).body().path("images")).hasSize(1);
      deliver(new Product.Updated(id, "Phone", "d", List.of(), 3));
      assertThat(get("/api/catalog/products/" + id).body().path("images").isArray()).isTrue();
      assertThat(get("/api/catalog/products/" + id).body().path("images")).isEmpty();

      boolean inTheFeed = false;
      for (JsonNode item : get("/api/catalog/products?size=100").body().path("items")) {
         if (item.path("id").asText().equals(id)) {
            inTheFeed = item.path("images").isArray();
         }
      }
      assertThat(inTheFeed).as("the feed carries images too").isTrue();
   }

   @Test
   void aProductShowsItsSkusPricesAndAvailabilityButNeverQuantities() throws Exception {
      String id = newProduct("Phone", "500.00");
      newSku(id, "sku-" + UUID.randomUUID(), "450.00", 7, 1);

      Reply reply = get("/api/catalog/products/" + id);

      assertThat(reply.status()).isEqualTo(200);
      JsonNode product = reply.body();
      assertThat(product.path("title").asText()).isEqualTo("Phone");
      assertThat(product.path("sellerId").asText()).isEqualTo("seller-1");
      assertThat(product.path("price").decimalValue()).isEqualByComparingTo("500.00");
      assertThat(product.path("lowestSkuPrice").decimalValue()).isEqualByComparingTo("450.00");
      assertThat(product.path("availability").asText()).isEqualTo("AVAILABLE");
      assertThat(product.path("createdAt").asText()).isNotBlank();
      JsonNode sku = product.path("skus").get(0);
      assertThat(sku.path("price").decimalValue()).isEqualByComparingTo("450.00");
      assertThat(sku.path("properties").path("color").asText()).isEqualTo("black");
      assertThat(sku.path("availability").asText()).isEqualTo("AVAILABLE");
      assertThat(reply.body().toString()).doesNotContain("tock").doesNotContain("uantity");
   }

   @Test
   void theFeedCanBeFilteredByAvailabilitySortedByPriceAndPaged() throws Exception {
      String cheap = newProduct("Cheap", "10.00");
      String middle = newProduct("Middle", "20.00");
      String pricey = newProduct("Pricey", "30.00");
      newSku(cheap, "sku-" + UUID.randomUUID(), "10.00", 1, 1);
      newSku(pricey, "sku-" + UUID.randomUUID(), "30.00", 0, 0);

      List<String> byPrice = ids(get("/api/catalog/products?sort=price,asc&size=100").body());
      assertThat(byPrice).containsSubsequence(cheap, middle, pricey);
      List<String> byPriceDesc = ids(get("/api/catalog/products?sort=price,desc&size=100").body());
      assertThat(byPriceDesc).containsSubsequence(pricey, middle, cheap);

      List<String> available = ids(get("/api/catalog/products?availability=AVAILABLE&size=100").body());
      assertThat(available).contains(cheap).doesNotContain(middle, pricey);
      List<String> outOfStock = ids(get("/api/catalog/products?availability=OUT_OF_STOCK&size=100").body());
      assertThat(outOfStock).contains(middle, pricey).doesNotContain(cheap);

      JsonNode firstPage = get("/api/catalog/products?size=2&page=0&sort=createdAt,desc").body();
      JsonNode secondPage = get("/api/catalog/products?size=2&page=1&sort=createdAt,desc").body();
      assertThat(firstPage.path("items")).hasSize(2);
      assertThat(firstPage.path("page").asInt()).isZero();
      assertThat(firstPage.path("size").asInt()).isEqualTo(2);
      assertThat(firstPage.path("totalElements").asLong()).isGreaterThanOrEqualTo(3);
      assertThat(firstPage.path("totalPages").asInt()).isGreaterThanOrEqualTo(2);
      assertThat(ids(firstPage)).doesNotContainAnyElementsOf(ids(secondPage));
   }

   @Test
   void deletedAndUnknownProductsAreNotFoundAndLeaveTheFeed() throws Exception {
      String id = newProduct("Gone", "10.00");
      assertThat(ids(get("/api/catalog/products?size=100").body())).contains(id);

      deliver(new Product.Deleted(id, 2));
      deliver(new Product.Updated(id, "Back?", "late", List.of(), 9));

      assertThat(ids(get("/api/catalog/products?size=100").body())).doesNotContain(id);
      assertThat(get("/api/catalog/products/" + id).status()).isEqualTo(404);
      assertThat(get("/api/catalog/products/" + UUID.randomUUID()).status()).isEqualTo(404);
   }

   @Test
   void aProductThatHasNotBeenCreatedYetIsNotShownEvenWhenItsSkuIs() throws Exception {
      String id = UUID.randomUUID().toString();
      newSku(id, "sku-" + UUID.randomUUID(), "5.00", 3, 1);
      assertThat(get("/api/catalog/products/" + id).status()).isEqualTo(404);

      deliver(new Product.Created(id, "seller-1", "Late bloomer", "d", new BigDecimal("6.00"), List.of(), 1));

      Reply reply = get("/api/catalog/products/" + id);
      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body().path("availability").asText()).isEqualTo("AVAILABLE");
      assertThat(reply.body().path("skus")).hasSize(1);
   }

   @Test
   void badParametersAreRejectedWith400() throws Exception {
      assertThat(get("/api/catalog/products?size=101").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?size=0").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?page=-1").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?page=abc").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?sort=title,asc").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?availability=SOLD_OUT").status()).isEqualTo(400);
      assertThat(get("/api/catalog/products?size=100&sort=price,desc&availability=AVAILABLE").status()).isEqualTo(200);
   }

   @Test
   void theIndexesForTheFeedExist() {
      List<String> keys = new ArrayList<>();
      database.getCollection("products").listIndexes().forEach(index -> keys.add(index.get("key").toString()));

      assertThat(keys).anyMatch(key -> key.contains("createdAt"))
            .anyMatch(key -> key.contains("listPrice"))
            .anyMatch(key -> key.contains("availability"));
   }

   @Test
   void duplicatesAndConcurrentDeliveriesOfTheSameEventsAreHarmless() throws Exception {
      String id = UUID.randomUUID().toString();
      String sku1 = "sku-" + UUID.randomUUID();
      String sku2 = "sku-" + UUID.randomUUID();
      List<DomainEvent> events = List.of(
            new Product.Created(id, "seller-1", "Busy", "d", new BigDecimal("9.00"), List.of(), 1),
            new Product.Updated(id, "Busy v2", "d2", List.of(), 2),
            new Sku.Created(sku1, id, "seller-1", new BigDecimal("8.00"), Map.of(), 1),
            new Sku.Created(sku2, id, "seller-1", new BigDecimal("7.00"), Map.of(), 1),
            new Inventory.StockAdded(sku1, 3, 3, 3, 1),
            new Inventory.AvailabilityChanged(sku1, 3, 3, 0, 2),
            new Inventory.StockAdded(sku2, 4, 4, 4, 1));

      ExecutorService pool = Executors.newFixedThreadPool(8);
      try {
         List<Future<Object>> running = new ArrayList<>();
         for (int round = 0; round < 3; round++) {
            for (DomainEvent event : events) {
               Callable<Object> delivery = () -> {
                  deliver(event);
                  return null;
               };
               running.add(pool.submit(delivery));
            }
         }
         for (Future<Object> delivery : running) {
            delivery.get();
         }
      } finally {
         pool.shutdown();
      }

      JsonNode product = get("/api/catalog/products/" + id).body();
      assertThat(product.path("title").asText()).isEqualTo("Busy v2");
      assertThat(product.path("skus")).hasSize(2);
      assertThat(product.path("lowestSkuPrice").decimalValue()).isEqualByComparingTo("7.00");
      assertThat(product.path("availability").asText()).isEqualTo("AVAILABLE");
      assertThat(database.getCollection("products").countDocuments(new org.bson.Document("_id", id))).isEqualTo(1);

      // sku2 is the only one left with stock; taking it away flips the product even though sku1 is reserved out
      deliver(new Inventory.AvailabilityChanged(sku2, 4, 4, 0, 2));
      assertThat(get("/api/catalog/products/" + id).body().path("availability").asText()).isEqualTo("OUT_OF_STOCK");
   }
}
