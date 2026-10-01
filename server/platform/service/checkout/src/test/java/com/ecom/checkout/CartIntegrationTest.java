package com.ecom.checkout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the cart against a real (H2) database. The catalog and inventory are replaced by a small stub so
 * drafting from the cart and committing can run too.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CartIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final UUID SELLER = UUID.randomUUID();
   private static final UUID PRODUCT = UUID.randomUUID();

   static final HttpServer STUB = startStub();

   private static HttpServer startStub() {
      try {
         HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
         server.createContext("/api/skus", CartIntegrationTest::catalog);
         server.createContext("/api/inventory/reservations", CartIntegrationTest::inventory);
         server.createContext("/api/auth/token", exchange -> respond(exchange, 200,
            "{\"accessToken\":\"stub-service-token\",\"tokenType\":\"Bearer\",\"expiresIn\":300}"));
         server.start();
         return server;
      } catch (IOException e) {
         throw new IllegalStateException(e);
      }
   }

   private static void catalog(HttpExchange exchange) throws IOException {
      String ids = exchange.getRequestURI().getQuery().substring("skuIds=".length());
      StringBuilder body = new StringBuilder("[");
      for (String id : ids.split(",")) {
         if (body.length() > 1) body.append(',');
         body.append("{\"skuId\":").append(id).append(",\"productId\":\"").append(PRODUCT)
            .append("\",\"sellerId\":\"").append(SELLER).append("\",\"price\":10.50}");
      }
      respond(exchange, 200, body.append(']').toString());
   }

   private static void inventory(HttpExchange exchange) throws IOException {
      String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      if (exchange.getRequestURI().getPath().equals("/api/inventory/reservations")) {
         StringBuilder reservations = new StringBuilder("{\"reservations\":[");
         JsonNode items = JSON.readTree(request).path("items");
         for (int i = 0; i < items.size(); i++) {
            if (i > 0) reservations.append(',');
            reservations.append("{\"reservationId\":\"").append(UUID.randomUUID())
               .append("\",\"skuId\":").append(items.get(i).path("skuId").asLong()).append('}');
         }
         respond(exchange, 201, reservations.append("]}").toString());
      } else {
         respond(exchange, 200, "{\"reservations\":[]}");
      }
   }

   private static void respond(HttpExchange exchange, int status, String body) throws IOException {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
   }

   @DynamicPropertySource
   static void stubUrls(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
      String base = "http://localhost:" + STUB.getAddress().getPort();
      registry.add("services.identity.base-url", () -> base);
      registry.add("services.inventory.base-url", () -> base);
      registry.add("services.listing.base-url", () -> base);
   }

   @AfterAll
   static void stopStub() {
      STUB.stop(0);
   }

   // ---- plumbing -----------------------------------------------------------------------------------------------

   @Value("${local.server.port}")
   int port;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
   }

   /** Calls as an admin, who may act for any customer; use the four-argument form to act as someone else or anonymously. */
   private Reply call(String method, String path, String body) throws Exception {
      return call(method, path, body, TestTokens.admin());
   }

   private Reply call(String method, String path, String body, String token) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
         .header("Content-Type", "application/json");
      if (token != null) {
         request.header("Authorization", "Bearer " + token);
      }
      request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json);
   }

   private Reply add(UUID customer, long sku, int quantity) throws Exception {
      return call("POST", "/api/carts/" + customer + "/items", "{\"skuId\":" + sku + ",\"quantity\":" + quantity + "}");
   }

   /** The cart as "sku:quantity" strings, ordered by SKU. */
   private List<String> lines(Reply cart) {
      List<String> lines = new ArrayList<>();
      cart.body().path("items").forEach(i -> lines.add(i.path("skuId").asLong() + ":" + i.path("quantity").asInt()));
      return lines;
   }

   // ---- reading ------------------------------------------------------------------------------------------------

   @Test
   void aCustomerWithoutACartGetsAnEmptyOneNotA404() throws Exception {
      UUID customer = UUID.randomUUID();

      Reply reply = call("GET", "/api/carts/" + customer, null);

      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body().path("customerId").asText()).isEqualTo(customer.toString());
      assertThat(reply.body().path("items")).isEmpty();
      assertThat(reply.body().path("version").asLong()).isZero();
   }

   // ---- writing ------------------------------------------------------------------------------------------------

   @Test
   void addingAccumulatesAndTheCartIsReadBack() throws Exception {
      UUID customer = UUID.randomUUID();

      add(customer, 5, 2);
      add(customer, 3, 1);
      Reply third = add(customer, 5, 4);

      assertThat(third.status()).isEqualTo(200);
      assertThat(lines(third)).containsExactly("3:1", "5:6");
      assertThat(third.body().path("updatedAt").asText()).isNotBlank();
      Reply read = call("GET", "/api/carts/" + customer, null);
      assertThat(lines(read)).containsExactly("3:1", "5:6");
      assertThat(read.body().path("version").asLong()).isEqualTo(third.body().path("version").asLong());
   }

   @Test
   void everyEditBumpsTheVersion() throws Exception {
      UUID customer = UUID.randomUUID();

      long first = add(customer, 5, 1).body().path("version").asLong();
      long second = add(customer, 5, 1).body().path("version").asLong();
      long third = call("PUT", "/api/carts/" + customer + "/items/5", "{\"quantity\":9}").body().path("version").asLong();

      assertThat(second).isGreaterThan(first);
      assertThat(third).isGreaterThan(second);
   }

   @Test
   void putSetsTheQuantityAndCreatesAMissingLine() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);

      Reply set = call("PUT", "/api/carts/" + customer + "/items/5", "{\"quantity\":9}");
      Reply created = call("PUT", "/api/carts/" + customer + "/items/8", "{\"quantity\":1}");

      assertThat(set.status()).isEqualTo(200);
      assertThat(lines(set)).containsExactly("5:9");
      assertThat(lines(created)).containsExactly("5:9", "8:1");
   }

   @Test
   void deleteRemovesOneSkuAndRemovingAMissingOneIsHarmless() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      add(customer, 6, 1);

      Reply removed = call("DELETE", "/api/carts/" + customer + "/items/5", null);
      Reply again = call("DELETE", "/api/carts/" + customer + "/items/5", null);

      assertThat(lines(removed)).containsExactly("6:1");
      assertThat(again.status()).isEqualTo(200);
      assertThat(lines(again)).containsExactly("6:1");
      assertThat(again.body().path("version").asLong()).isEqualTo(removed.body().path("version").asLong());
   }

   @Test
   void deleteOnTheCartClearsIt() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      add(customer, 6, 1);

      Reply cleared = call("DELETE", "/api/carts/" + customer, null);

      assertThat(cleared.status()).isEqualTo(200);
      assertThat(cleared.body().path("items")).isEmpty();
      assertThat(call("GET", "/api/carts/" + customer, null).body().path("items")).isEmpty();
      assertThat(call("DELETE", "/api/carts/" + UUID.randomUUID(), null).status()).as("clearing a cart nobody made").isEqualTo(200);
   }

   @Test
   void cartsOfDifferentCustomersAreIndependent() throws Exception {
      UUID one = UUID.randomUUID();
      UUID two = UUID.randomUUID();

      add(one, 5, 1);

      assertThat(call("GET", "/api/carts/" + two, null).body().path("items")).isEmpty();
   }

   // ---- validation ---------------------------------------------------------------------------------------------

   @Test
   void inputsAreValidated() throws Exception {
      UUID customer = UUID.randomUUID();
      String items = "/api/carts/" + customer + "/items";

      assertThat(add(customer, 5, 0).status()).isEqualTo(400);
      assertThat(add(customer, 5, -1).status()).isEqualTo(400);
      assertThat(add(customer, 5, 1001).status()).isEqualTo(400);
      Reply noSku = call("POST", items, "{\"quantity\":1}");
      assertThat(noSku.status()).isEqualTo(400);
      assertThat(noSku.body().path("invalidFields").has("skuId")).isTrue();
      assertThat(call("POST", items, "{\"skuId\":5}").status()).isEqualTo(400);
      assertThat(call("PUT", items + "/5", "{\"quantity\":0}").status()).isEqualTo(400);
      assertThat(call("PUT", items + "/5", "{}").status()).isEqualTo(400);
      assertThat(call("GET", "/api/carts/not-a-uuid", null).status()).isEqualTo(400);
      assertThat(call("GET", "/api/carts/" + customer, null).body().path("items")).as("nothing was stored").isEmpty();
   }

   @Test
   void aLineCannotGrowPastTheQuantityLimitByAdding() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 800);

      Reply reply = add(customer, 5, 300);

      assertThat(reply.status()).isEqualTo(400);
      assertThat(lines(call("GET", "/api/carts/" + customer, null))).containsExactly("5:800");
   }

   @Test
   void aCartHoldsAtMostFiftyDifferentItems() throws Exception {
      UUID customer = UUID.randomUUID();
      for (long sku = 1; sku <= 50; sku++) {
         assertThat(add(customer, sku, 1).status()).isEqualTo(200);
      }

      assertThat(add(customer, 51, 1).status()).isEqualTo(400);
      assertThat(call("PUT", "/api/carts/" + customer + "/items/51", "{\"quantity\":1}").status()).isEqualTo(400);
      assertThat(add(customer, 50, 1).status()).as("an existing line can still grow").isEqualTo(200);
   }

   // ---- versions -----------------------------------------------------------------------------------------------

   @Test
   void aStaleVersionIsA409AndTheCartIsUntouched() throws Exception {
      UUID customer = UUID.randomUUID();
      long seen = add(customer, 5, 1).body().path("version").asLong();
      add(customer, 5, 1); // someone else edits

      Reply post = call("POST", "/api/carts/" + customer + "/items", "{\"skuId\":5,\"quantity\":1,\"version\":" + seen + "}");
      Reply put = call("PUT", "/api/carts/" + customer + "/items/5", "{\"quantity\":9,\"version\":" + seen + "}");
      Reply delete = call("DELETE", "/api/carts/" + customer + "/items/5?version=" + seen, null);
      Reply clear = call("DELETE", "/api/carts/" + customer + "?version=" + seen, null);

      for (Reply reply : List.of(post, put, delete, clear)) {
         assertThat(reply.status()).isEqualTo(409);
         assertThat(reply.body().path("errorCode").asText()).isEqualTo("VERSION_CONFLICT");
      }
      assertThat(lines(call("GET", "/api/carts/" + customer, null))).containsExactly("5:2");
   }

   @Test
   void theCurrentVersionIsAccepted() throws Exception {
      UUID customer = UUID.randomUUID();
      long seen = add(customer, 5, 1).body().path("version").asLong();

      Reply reply = call("PUT", "/api/carts/" + customer + "/items/5", "{\"quantity\":4,\"version\":" + seen + "}");

      assertThat(reply.status()).isEqualTo(200);
      assertThat(lines(reply)).containsExactly("5:4");
   }

   // ---- who may do what ----------------------------------------------------------------------------------------

   @Test
   void aCartBelongsToItsCustomerAndOnlyAnAdminMayActForAnother() throws Exception {
      UUID customer = UUID.randomUUID();
      String path = "/api/carts/" + customer;
      String body = "{\"skuId\":5,\"quantity\":2}";
      String other = TestTokens.buyer(UUID.randomUUID());

      assertThat(call("GET", path, null, null).status()).as("anonymous").isEqualTo(401);
      assertThat(call("GET", path, null, other).status()).as("someone else's cart").isEqualTo(403);
      assertThat(call("POST", path + "/items", body, other).status()).isEqualTo(403);
      assertThat(call("DELETE", path, null, other).status()).isEqualTo(403);
      assertThat(call("GET", path, null, TestTokens.system()).status()).as("a service is not a customer").isEqualTo(403);

      assertThat(call("POST", path + "/items", body, TestTokens.buyer(customer)).status()).as("the owner").isEqualTo(200);
      assertThat(call("GET", path, null, TestTokens.buyer(customer)).status()).isEqualTo(200);
      assertThat(call("GET", path, null, TestTokens.admin()).status()).as("admin").isEqualTo(200);
      assertThat(lines(call("GET", path, null, TestTokens.admin()))).containsExactly("5:2");
   }

   // ---- checkout -----------------------------------------------------------------------------------------------

   private Reply draftFromCart(UUID customer, String itemsField) throws Exception {
      return call("POST", "/api/checkouts", "{" + itemsField.replaceFirst("^,", "") + "}", TestTokens.buyer(customer));
   }

   @Test
   void aDraftWithoutItemsUsesTheCart() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      add(customer, 6, 1);

      for (String itemsField : List.of("", ",\"items\":[]", ",\"items\":null")) {
         Reply draft = draftFromCart(customer, itemsField);

         assertThat(draft.status()).as(itemsField).isEqualTo(201);
         assertThat(draft.body().path("items")).hasSize(2);
         assertThat(draft.body().path("items").get(0).path("skuId").asLong()).isEqualTo(5);
         assertThat(draft.body().path("items").get(0).path("quantity").asInt()).isEqualTo(2);
         assertThat(draft.body().path("totalAmount").decimalValue()).isEqualByComparingTo("31.50");
      }
      assertThat(lines(call("GET", "/api/carts/" + customer, null))).as("drafting leaves the cart alone").containsExactly("5:2", "6:1");
   }

   @Test
   void aDraftWithNeitherItemsNorACartIsA400() throws Exception {
      UUID customer = UUID.randomUUID();

      assertThat(draftFromCart(customer, "").status()).isEqualTo(400);
      add(customer, 5, 1);
      call("DELETE", "/api/carts/" + customer, null);
      assertThat(draftFromCart(customer, ",\"items\":[]").status()).as("an emptied cart").isEqualTo(400);
   }

   @Test
   void explicitItemsStillWinOverTheCart() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);

      Reply draft = draftFromCart(customer, ",\"items\":[{\"skuId\":9,\"quantity\":1}]");

      assertThat(draft.status()).isEqualTo(201);
      assertThat(draft.body().path("items")).hasSize(1);
      assertThat(draft.body().path("items").get(0).path("skuId").asLong()).isEqualTo(9);
   }

   @Test
   void committingRemovesOnlyTheCommittedSkusFromTheCart() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      add(customer, 6, 1);
      add(customer, 7, 3);
      Reply draft = draftFromCart(customer, ",\"items\":[{\"skuId\":5,\"quantity\":2},{\"skuId\":7,\"quantity\":1}]");
      String checkoutId = draft.body().path("checkoutId").asText();
      long before = call("GET", "/api/carts/" + customer, null).body().path("version").asLong();

      Reply commit = call("POST", "/api/checkouts/" + checkoutId + "/commit", null);

      assertThat(commit.status()).isEqualTo(200);
      Reply cart = call("GET", "/api/carts/" + customer, null);
      assertThat(lines(cart)).containsExactly("6:1");
      assertThat(cart.body().path("version").asLong()).isGreaterThan(before);
   }

   @Test
   void committingACheckoutFromTheWholeCartEmptiesIt() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      add(customer, 6, 1);
      String checkoutId = draftFromCart(customer, "").body().path("checkoutId").asText();

      call("POST", "/api/checkouts/" + checkoutId + "/commit", null);
      Reply replay = call("POST", "/api/checkouts/" + checkoutId + "/commit", null);

      assertThat(call("GET", "/api/carts/" + customer, null).body().path("items")).isEmpty();
      assertThat(replay.status()).isEqualTo(200);
   }

   @Test
   void committingWithoutACartWorksAsBefore() throws Exception {
      UUID customer = UUID.randomUUID();
      String checkoutId = draftFromCart(customer, ",\"items\":[{\"skuId\":5,\"quantity\":1}]").body().path("checkoutId").asText();

      Reply commit = call("POST", "/api/checkouts/" + checkoutId + "/commit", null);

      assertThat(commit.status()).isEqualTo(200);
      assertThat(call("GET", "/api/carts/" + customer, null).body().path("items")).isEmpty();
   }

   @Test
   void cancellingLeavesTheCartAlone() throws Exception {
      UUID customer = UUID.randomUUID();
      add(customer, 5, 2);
      String checkoutId = draftFromCart(customer, "").body().path("checkoutId").asText();

      Reply cancel = call("POST", "/api/checkouts/" + checkoutId + "/cancel", null);

      assertThat(cancel.status()).isEqualTo(200);
      assertThat(lines(call("GET", "/api/carts/" + customer, null))).containsExactly("5:2");
   }
}
