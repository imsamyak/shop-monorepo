package com.ecom.checkout;

import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import com.ecom.checkout.service.record.CheckoutExpiryScheduler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of checkout against a real (H2) database, with the catalog and inventory replaced by a local stub
 * HTTP server so their behaviour (rejections, outages, slowness) can be controlled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CheckoutIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final UUID SELLER_A = UUID.randomUUID();
   private static final UUID SELLER_B = UUID.randomUUID();
   private static final UUID PRODUCT = UUID.randomUUID();

   // ---- stub of the catalog and inventory ----------------------------------------------------------------------

   enum InventoryMode {OK, REJECT, DOWN, SLOW}

   static volatile InventoryMode inventoryMode = InventoryMode.OK;
   static final List<String> inventoryCalls = new CopyOnWriteArrayList<>();
   static final List<String> inventoryAuthHeaders = new CopyOnWriteArrayList<>();

   static final HttpServer STUB = startStub();

   private static HttpServer startStub() {
      try {
         HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
         server.createContext("/skus", CheckoutIntegrationTest::catalog);
         server.createContext("/inventory/reservations", CheckoutIntegrationTest::inventory);
         server.createContext("/auth/token", exchange -> respond(exchange, 200,
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
         long sku = Long.parseLong(id);
         if (sku == 404) {
            continue; // unknown SKU
         }
         UUID seller = sku % 2 == 0 ? SELLER_A : SELLER_B;
         if (body.length() > 1) body.append(',');
         body.append("{\"skuId\":").append(sku).append(",\"productId\":\"").append(PRODUCT)
            .append("\",\"sellerId\":\"").append(seller).append("\",\"price\":").append(sku * 10).append(".50}");
      }
      respond(exchange, 200, body.append(']').toString());
   }

   private static void inventory(HttpExchange exchange) throws IOException {
      inventoryCalls.add(exchange.getRequestURI().getPath());
      inventoryAuthHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
      String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      switch (inventoryMode) {
         case DOWN -> respond(exchange, 500, "{\"detail\":\"boom\"}");
         case REJECT -> respond(exchange, 409, "{\"detail\":\"Insufficient stock for SKU 2\"}");
         case SLOW -> {
            try {
               Thread.sleep(2_000);
            } catch (InterruptedException ignored) {
               Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{}");
         }
         default -> {
            if (exchange.getRequestURI().getPath().equals("/inventory/reservations")) {
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
      String base = "http://localhost:" + STUB.getAddress().getPort();
      TestTokens.register(registry);
      registry.add("services.identity.base-url", () -> base);
      registry.add("services.inventory.base-url", () -> base);
      registry.add("services.listing.base-url", () -> base);
      registry.add("services.inventory.read-timeout-ms", () -> 500);
   }

   @AfterAll
   static void stopStub() {
      STUB.stop(0);
   }

   // ---- plumbing -----------------------------------------------------------------------------------------------

   @Value("${local.server.port}")
   int port;

   @Autowired
   JdbcTemplate jdbc;

   @Autowired
   LoadCheckoutPort loadCheckoutPort;

   @Autowired
   RecordCheckouts.RecordExpiry recordExpiry;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
   }

   @BeforeEach
   void reset() {
      inventoryMode = InventoryMode.OK;
      inventoryCalls.clear();
      inventoryAuthHeaders.clear();
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

   private Reply draft(UUID customer, String itemsJson) throws Exception {
      return call("POST", "/checkouts", "{\"items\":" + itemsJson + "}", TestTokens.buyer(customer));
   }

   private String draftId(UUID customer) throws Exception {
      Reply reply = draft(customer, "[{\"skuId\":2,\"quantity\":1}]");
      assertThat(reply.status()).isEqualTo(201);
      return reply.body().path("checkoutId").asText();
   }

   private List<String> events(String checkoutId, String action) {
      return jdbc.queryForList("select payload from outbox_events where aggregate_id = ? and payload like ?",
         String.class, checkoutId, "%\"action\":\"" + action + "\"%");
   }

   private String status(String checkoutId) {
      return jdbc.queryForObject("select status from checkouts where id = ?", String.class, UUID.fromString(checkoutId));
   }

   @Test
   void eachCheckoutEventCarriesTheCheckoutVersionAfterItsChange() throws Exception {
      String id = draftId(UUID.randomUUID());
      call("POST", "/checkouts/" + id + "/commit", null);

      long drafted = JSON.readTree(events(id, "drafted").get(0)).path("data").path("version").asLong();
      long committed = JSON.readTree(events(id, "committed").get(0)).path("data").path("version").asLong();

      assertThat(committed).isGreaterThan(drafted);
   }

   // ---- drafting -----------------------------------------------------------------------------------------------

   @Test
   void draftUsesCatalogPricesAndTheGivenCustomerAndIgnoresClientSuppliedPrices() throws Exception {
      UUID customer = UUID.randomUUID();

      Reply reply = call("POST", "/checkouts",
         "{\"items\":[{\"skuId\":2,\"quantity\":3,\"unitPrice\":0.01},"
            + "{\"skuId\":3,\"quantity\":1,\"unitPrice\":0.01}]}", TestTokens.buyer(customer));

      assertThat(reply.status()).isEqualTo(201);
      assertThat(reply.body().path("customerId").asText()).isEqualTo(customer.toString());
      assertThat(reply.body().path("status").asText()).isEqualTo("DRAFT");
      JsonNode items = reply.body().path("items");
      assertThat(items).hasSize(2);
      assertThat(items.get(0).path("unitPrice").decimalValue()).isEqualByComparingTo("20.50");
      assertThat(items.get(0).path("sellerId").asText()).isEqualTo(SELLER_A.toString());
      assertThat(items.get(1).path("sellerId").asText()).isEqualTo(SELLER_B.toString());
      assertThat(reply.body().path("totalAmount").decimalValue()).isEqualByComparingTo("92.00"); // 3 x 20.50 + 1 x 30.50
      assertThat(inventoryAuthHeaders).as("inventory is called as this service").containsExactly("Bearer stub-service-token");
      assertThat(events(reply.body().path("checkoutId").asText(), "drafted")).hasSize(1);
   }

   @Test
   void unknownSkusAreRejectedBeforeStockIsTouched() throws Exception {
      Reply reply = draft(UUID.randomUUID(), "[{\"skuId\":404,\"quantity\":1}]");

      assertThat(reply.status()).isEqualTo(422);
      assertThat(inventoryCalls).isEmpty();
   }

   @Test
   void whenInventoryRejectsTheReservationNothingIsRecorded() throws Exception {
      inventoryMode = InventoryMode.REJECT;
      UUID customer = UUID.randomUUID();

      Reply reply = draft(customer, "[{\"skuId\":2,\"quantity\":1}]");

      assertThat(reply.status()).isEqualTo(422);
      assertThat(reply.body().path("detail").asText()).contains("Insufficient stock");
      assertThat(jdbc.queryForObject("select count(*) from checkouts where customer_id = ?", Integer.class, customer)).isZero();
   }

   @Test
   void anInventoryOutageIsA503NotA500() throws Exception {
      inventoryMode = InventoryMode.DOWN;

      Reply reply = draft(UUID.randomUUID(), "[{\"skuId\":2,\"quantity\":1}]");

      assertThat(reply.status()).isEqualTo(503);
      assertThat(reply.body().path("errorCode").asText()).isEqualTo("SERVICE_UNAVAILABLE");
   }

   @Test
   void aSlowInventoryTimesOutInsteadOfHangingTheRequest() throws Exception {
      inventoryMode = InventoryMode.SLOW;

      long start = System.nanoTime();
      Reply reply = draft(UUID.randomUUID(), "[{\"skuId\":2,\"quantity\":1}]");

      assertThat(reply.status()).isEqualTo(503);
      assertThat((System.nanoTime() - start) / 1_000_000).as("returned well before the stub's 2s delay").isLessThan(1_800);
   }

   @Test
   void draftInputsAreValidated() throws Exception {
      UUID customer = UUID.randomUUID();

      assertThat(draft(customer, "[]").status()).isEqualTo(400);
      assertThat(draft(customer, "[{\"skuId\":2,\"quantity\":0}]").status()).isEqualTo(400);
      assertThat(call("POST", "/checkouts",
         "{\"items\":[{\"skuId\":2,\"quantity\":1}],\"ttlSeconds\":999999}", TestTokens.buyer(customer)).status()).isEqualTo(400);
      assertThat(inventoryCalls).isEmpty();
   }

   @Test
   void onlySignedInUsersCanDraftAndTheCustomerIsTheCaller() throws Exception {
      String items = "{\"items\":[{\"skuId\":2,\"quantity\":1}]}";

      assertThat(call("POST", "/checkouts", items, null).status()).as("anonymous").isEqualTo(401);
      assertThat(call("POST", "/checkouts", items, TestTokens.system()).status()).as("a service is not a customer").isEqualTo(403);
      assertThat(inventoryCalls).isEmpty();

      UUID customer = UUID.randomUUID();
      Reply reply = call("POST", "/checkouts", "{\"customerId\":\"" + UUID.randomUUID() + "\",\"items\":[{\"skuId\":2,\"quantity\":1}]}",
         TestTokens.buyer(customer));
      assertThat(reply.status()).isEqualTo(201);
      assertThat(reply.body().path("customerId").asText()).as("a customerId in the body is ignored").isEqualTo(customer.toString());
   }

   @Test
   void aCheckoutCanOnlyBeReadCommittedOrCancelledByItsCustomerOrAnAdmin() throws Exception {
      UUID customer = UUID.randomUUID();
      String id = draftId(customer);
      String other = TestTokens.buyer(UUID.randomUUID());
      inventoryCalls.clear();

      assertThat(call("GET", "/checkouts/" + id, null, null).status()).isEqualTo(401);
      assertThat(call("GET", "/checkouts/" + id, null, other).status()).isEqualTo(403);
      assertThat(call("POST", "/checkouts/" + id + "/commit", null, other).status()).isEqualTo(403);
      assertThat(call("POST", "/checkouts/" + id + "/cancel", null, other).status()).isEqualTo(403);
      assertThat(inventoryCalls).as("nothing reached inventory").isEmpty();

      assertThat(call("GET", "/checkouts/" + id, null, TestTokens.buyer(customer)).status()).isEqualTo(200);
      assertThat(call("POST", "/checkouts/" + id + "/commit", null, TestTokens.buyer(customer)).status()).isEqualTo(200);
      assertThat(call("GET", "/checkouts/" + id, null, TestTokens.admin()).status()).isEqualTo(200);
   }

   // ---- reading ------------------------------------------------------------------------------------------------

   @Test
   void aCheckoutCanBeReadBackAndAnUnknownOneIsNotFound() throws Exception {
      UUID customer = UUID.randomUUID();
      String id = draftId(customer);

      Reply found = call("GET", "/checkouts/" + id, null);
      assertThat(found.status()).isEqualTo(200);
      assertThat(found.body().path("customerId").asText()).isEqualTo(customer.toString());

      assertThat(call("GET", "/checkouts/" + UUID.randomUUID(), null).status()).isEqualTo(404);
   }

   // ---- commit -------------------------------------------------------------------------------------------------

   @Test
   void commitConsumesStockOnceRecordsAnEventWithSellersAndIsIdempotent() throws Exception {
      Reply drafted = draft(UUID.randomUUID(), "[{\"skuId\":2,\"quantity\":1},{\"skuId\":3,\"quantity\":2}]");
      String id = drafted.body().path("checkoutId").asText();
      inventoryCalls.clear();

      Reply first = call("POST", "/checkouts/" + id + "/commit", null);
      Reply second = call("POST", "/checkouts/" + id + "/commit", null);

      assertThat(first.status()).isEqualTo(200);
      assertThat(first.body().path("status").asText()).isEqualTo("COMMITTED");
      assertThat(second.status()).isEqualTo(200);
      assertThat(inventoryCalls).as("the retry did not call inventory again").containsExactly("/inventory/reservations/commit");
      List<String> committed = events(id, "committed");
      assertThat(committed).as("one event, not one per call").hasSize(1);
      JsonNode data = JSON.readTree(committed.get(0)).path("data");
      assertThat(data.path("items")).hasSize(2);
      assertThat(data.path("items").get(0).path("sellerId").asText()).isNotBlank();
   }

   @Test
   void whenInventoryRefusesTheCommitTheCheckoutStaysADraft() throws Exception {
      String id = draftId(UUID.randomUUID());
      inventoryMode = InventoryMode.REJECT;

      Reply commit = call("POST", "/checkouts/" + id + "/commit", null);

      assertThat(commit.status()).isEqualTo(422);
      assertThat(status(id)).isEqualTo("DRAFT");
      assertThat(events(id, "committed")).isEmpty();
   }

   @Test
   void anExpiredDraftCannotBeCommittedAndInventoryIsNotCalled() throws Exception {
      String id = draftId(UUID.randomUUID());
      jdbc.update("update checkouts set expires_at = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(60)), UUID.fromString(id));
      inventoryCalls.clear();

      Reply commit = call("POST", "/checkouts/" + id + "/commit", null);

      assertThat(commit.status()).isEqualTo(422);
      assertThat(inventoryCalls).isEmpty();
   }

   // ---- cancel -------------------------------------------------------------------------------------------------

   @Test
   void cancelReleasesStockAndIsIdempotent() throws Exception {
      String id = draftId(UUID.randomUUID());
      inventoryCalls.clear();

      Reply first = call("POST", "/checkouts/" + id + "/cancel", null);
      Reply second = call("POST", "/checkouts/" + id + "/cancel", null);

      assertThat(first.body().path("status").asText()).isEqualTo("CANCELLED");
      assertThat(second.status()).isEqualTo(200);
      assertThat(inventoryCalls).containsExactly("/inventory/reservations/release");
      assertThat(events(id, "cancelled")).hasSize(1);
      assertThat(call("POST", "/checkouts/" + id + "/commit", null).status()).as("cannot commit a cancelled checkout").isEqualTo(422);
   }

   @Test
   void aFailedReleaseIsReportedAndTheCheckoutIsNotMarkedCancelled() throws Exception {
      String id = draftId(UUID.randomUUID());
      inventoryMode = InventoryMode.DOWN;

      Reply cancel = call("POST", "/checkouts/" + id + "/cancel", null);

      assertThat(cancel.status()).isEqualTo(503);
      assertThat(status(id)).isEqualTo("DRAFT");

      inventoryMode = InventoryMode.OK;
      assertThat(call("POST", "/checkouts/" + id + "/cancel", null).status()).as("the retry succeeds").isEqualTo(200);
   }

   // ---- expiry -------------------------------------------------------------------------------------------------

   @Test
   void theExpirySweepMarksOverdueDraftsExpiredAndEmitsACancellation() throws Exception {
      String id = draftId(UUID.randomUUID());
      jdbc.update("update checkouts set expires_at = ? where id = ?", Timestamp.from(Instant.now().minusSeconds(60)), UUID.fromString(id));

      new CheckoutExpiryScheduler(loadCheckoutPort, recordExpiry).expireOverdueDrafts();

      assertThat(status(id)).isEqualTo("EXPIRED");
      List<String> cancelled = events(id, "cancelled");
      assertThat(cancelled).hasSize(1);
      assertThat(cancelled.get(0)).contains("EXPIRED");
   }
}
