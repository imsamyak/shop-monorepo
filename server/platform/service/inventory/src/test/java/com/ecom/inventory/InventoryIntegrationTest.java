package com.ecom.inventory;

import com.ecom.inbox.InboxListener;
import com.ecom.inventory.port.in.usecase.reservation.ExpireReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ExpireReservationsCmd;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of inventory against a real (H2) database: SKU events create stock, reservations are atomic and
 * idempotent, and expiry returns stock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final AtomicLong SKU_IDS = new AtomicLong(1_000);

   @DynamicPropertySource
   static void jwt(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   JdbcTemplate jdbc;

   @Autowired
   InboxListener inboxListener;

   @Autowired
   ExpireReservationsUseCase expireReservations;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
      String text(String field) {
         return body.path(field).asText();
      }
   }

   /** Calls as an admin, who may do everything; use the four-argument form to act as someone else or anonymously. */
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

   /**
    * Creates a SKU through the same inbox path production uses (Sku.Created event).
    */
   private long newSku(UUID sellerId) {
      long skuId = SKU_IDS.incrementAndGet();
      String payload = "{\"skuId\":\"" + skuId + "\",\"productId\":\"" + UUID.randomUUID()
         + "\",\"sellerId\":\"" + sellerId + "\",\"price\":10.00}";
      inboxListener.listen("msg-" + UUID.randomUUID(), "sku.created", payload);
      return skuId;
   }

   private long newSkuWithStock(UUID sellerId, int quantity) throws Exception {
      long skuId = newSku(sellerId);
      Reply added = call("POST", "/api/inventory/" + skuId + "/stock/add", "{\"quantity\":" + quantity + "}");
      assertThat(added.status()).isEqualTo(200);
      return skuId;
   }

   private String reserveBody(String reference, long ttl, long... skuAndQty) {
      StringBuilder items = new StringBuilder();
      for (int i = 0; i < skuAndQty.length; i += 2) {
         if (i > 0) items.append(',');
         items.append("{\"skuId\":").append(skuAndQty[i]).append(",\"quantity\":").append(skuAndQty[i + 1]).append('}');
      }
      return "{\"referenceId\":\"" + reference + "\",\"ttlSeconds\":" + ttl + ",\"items\":[" + items + "]}";
   }

   private int reserved(long skuId) {
      return jdbc.queryForObject("select reserved_quantity from inventory_items where sku_id = ?", Integer.class, skuId);
   }

   private int total(long skuId) {
      return jdbc.queryForObject("select total_quantity from inventory_items where sku_id = ?", Integer.class, skuId);
   }

   // ------------------------------------------------------------------ stock

   @Test
   void skuCreatedEventCreatesAnEmptyItemOwnedByTheSellerAndIsIdempotent() {
      UUID sellerId = UUID.randomUUID();
      long skuId = newSku(sellerId);

      assertThat(total(skuId)).isZero();
      assertThat(jdbc.queryForObject("select seller_id from inventory_items where sku_id = ?", UUID.class, skuId)).isEqualTo(sellerId);

      // the same SKU announced again (different message id) must not create a duplicate
      inboxListener.listen("msg-again", "sku.created",
         "{\"skuId\":\"" + skuId + "\",\"productId\":\"" + UUID.randomUUID() + "\",\"sellerId\":\"" + sellerId + "\",\"price\":1}");
      assertThat(jdbc.queryForObject("select count(*) from inventory_items where sku_id = ?", Integer.class, skuId)).isEqualTo(1);
   }

   @Test
   void duplicateInboxMessagesAreProcessedOnce() {
      UUID sellerId = UUID.randomUUID();
      long skuId = SKU_IDS.incrementAndGet();
      String payload = "{\"skuId\":\"" + skuId + "\",\"productId\":\"" + UUID.randomUUID() + "\",\"sellerId\":\"" + sellerId + "\",\"price\":1}";

      inboxListener.listen("same-message", "sku.created", payload);
      inboxListener.listen("same-message", "sku.created", payload);

      assertThat(jdbc.queryForObject("select count(*) from inbox where message_id = 'same-message'", Integer.class)).isEqualTo(1);
   }

   @Test
   void stockCanOnlyBeAddedToExistingSkusAndWithinLimits() throws Exception {
      long skuId = newSku(UUID.randomUUID());

      assertThat(call("POST", "/api/inventory/" + skuId + "/stock/add", "{\"quantity\":5}").status()).isEqualTo(200);
      assertThat(call("POST", "/api/inventory/9999999/stock/add", "{\"quantity\":5}").status()).isEqualTo(404);
      assertThat(call("POST", "/api/inventory/" + skuId + "/stock/add", "{\"quantity\":2000000}").status()).isEqualTo(400);
      assertThat(call("POST", "/api/inventory/" + skuId + "/stock/remove", "{\"quantity\":2}").status()).isEqualTo(200);
      assertThat(total(skuId)).isEqualTo(3);
   }

   @Test
   void stockChangesAreAnnouncedThroughTheOutbox() throws Exception {
      long skuId = newSku(UUID.randomUUID());

      call("POST", "/api/inventory/" + skuId + "/stock/add", "{\"quantity\":5}");

      assertThat(jdbc.queryForObject(
         "select count(*) from outbox_events where aggregate_type = 'inventory' and aggregate_id = ?",
         Integer.class, String.valueOf(skuId))).isEqualTo(1);
   }

   /** The announced events of one aggregate, oldest first, as {@code {action, data}} envelopes. */
   private List<JsonNode> announced(String aggregateType, String aggregateId) throws Exception {
      List<JsonNode> events = new java.util.ArrayList<>();
      for (String payload : jdbc.queryForList(
         "select payload from outbox_events where aggregate_type = ? and aggregate_id = ? order by created_at",
         String.class, aggregateType, aggregateId)) {
         events.add(JSON.readTree(payload));
      }
      return events;
   }

   @Test
   void stockEventsCarryAGrowingVersionAndTheStockAfterEveryReservationChange() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 60, sku, 4));
      String reservationId = reserved.body().path("reservations").get(0).path("reservationId").asText();
      call("POST", "/api/inventory/reservations/commit", "{\"reservationIds\":[\"" + reservationId + "\"]}");
      Reply second = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 60, sku, 3));
      String secondId = second.body().path("reservations").get(0).path("reservationId").asText();
      call("POST", "/api/inventory/reservations/release", "{\"reservationIds\":[\"" + secondId + "\"]}");

      List<JsonNode> events = announced("inventory", String.valueOf(sku));

      assertThat(events.stream().map(e -> e.path("action").asText()).toList()).containsExactly(
         "stock-added", "availability-changed", "availability-changed", "availability-changed", "availability-changed");
      List<Long> versions = events.stream().map(e -> e.path("data").path("version").asLong()).toList();
      assertThat(versions).isSorted().doesNotHaveDuplicates();
      // reserved 4 of 10 -> committed (total 6) -> reserved 3 -> released
      assertThat(events.get(0).path("data").path("availableStock").asInt()).isEqualTo(10);
      assertThat(events.get(1).path("data").path("reservedStock").asInt()).isEqualTo(4);
      assertThat(events.get(1).path("data").path("availableStock").asInt()).isEqualTo(6);
      assertThat(events.get(2).path("data").path("totalStock").asInt()).isEqualTo(6);
      assertThat(events.get(2).path("data").path("reservedStock").asInt()).isZero();
      assertThat(events.get(3).path("data").path("availableStock").asInt()).isEqualTo(3);
      assertThat(events.get(4).path("data").path("availableStock").asInt()).isEqualTo(6);

      List<JsonNode> reservation = announced("reservation", reservationId);
      assertThat(reservation.stream().map(e -> e.path("action").asText()).toList()).containsExactly("created", "committed");
      assertThat(reservation.get(1).path("data").path("version").asLong())
         .isGreaterThan(reservation.get(0).path("data").path("version").asLong());
   }

   // ------------------------------------------------------------ who may do what

   @Test
   void stockCanOnlyBeChangedByTheSellerWhoOwnsTheSkuOrAnAdmin() throws Exception {
      UUID owner = UUID.randomUUID();
      long sku = newSku(owner);
      String add = "{\"quantity\":5}";
      String path = "/api/inventory/" + sku + "/stock/add";

      assertThat(call("POST", path, add, null).status()).as("anonymous").isEqualTo(401);
      assertThat(call("POST", path, add, TestTokens.buyer(UUID.randomUUID())).status()).as("buyer").isEqualTo(403);
      assertThat(call("POST", path, add, TestTokens.system()).status()).as("other service").isEqualTo(403);
      assertThat(call("POST", path, add, TestTokens.seller(UUID.randomUUID())).status()).as("another seller").isEqualTo(403);
      assertThat(total(sku)).isZero();

      assertThat(call("POST", path, add, TestTokens.seller(owner)).status()).as("owner").isEqualTo(200);
      assertThat(call("POST", path, add, TestTokens.admin()).status()).as("admin").isEqualTo(200);
      assertThat(total(sku)).isEqualTo(10);
   }

   @Test
   void aSellerReadsOnlyTheirOwnStockAndBulkReadsAreForAdminsAndServices() throws Exception {
      UUID owner = UUID.randomUUID();
      long sku = newSkuWithStock(owner, 3);

      assertThat(call("GET", "/api/inventory/" + sku, null, null).status()).isEqualTo(401);
      assertThat(call("GET", "/api/inventory/" + sku, null, TestTokens.buyer(UUID.randomUUID())).status()).isEqualTo(403);
      assertThat(call("GET", "/api/inventory/" + sku, null, TestTokens.seller(UUID.randomUUID())).status()).isEqualTo(403);
      assertThat(call("GET", "/api/inventory/" + sku, null, TestTokens.seller(owner)).status()).isEqualTo(200);
      assertThat(call("GET", "/api/inventory/" + sku, null, TestTokens.system()).status()).isEqualTo(200);

      assertThat(call("GET", "/api/inventory", null, TestTokens.seller(owner)).status()).as("a seller cannot list everyone's stock").isEqualTo(403);
      assertThat(call("GET", "/api/inventory?skuIds=" + sku, null, TestTokens.seller(owner)).status()).isEqualTo(403);
      assertThat(call("GET", "/api/inventory", null, TestTokens.admin()).status()).isEqualTo(200);
      assertThat(call("GET", "/api/inventory?skuIds=" + sku, null, TestTokens.system()).status()).isEqualTo(200);
   }

   @Test
   void reservationsAreForOtherServicesAndAdminsOnly() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      String body = reserveBody("chk-" + UUID.randomUUID(), 60, sku, 2);

      assertThat(call("POST", "/api/inventory/reservations", body, null).status()).isEqualTo(401);
      assertThat(call("POST", "/api/inventory/reservations", body, TestTokens.buyer(UUID.randomUUID())).status()).isEqualTo(403);
      assertThat(call("POST", "/api/inventory/reservations", body, TestTokens.seller(UUID.randomUUID())).status()).isEqualTo(403);
      Reply reserved = call("POST", "/api/inventory/reservations", body, TestTokens.system());
      assertThat(reserved.status()).isEqualTo(201);

      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();
      assertThat(call("GET", "/api/inventory/reservations/" + id, null, TestTokens.seller(UUID.randomUUID())).status()).isEqualTo(403);
      assertThat(call("GET", "/api/inventory/reservations/" + id, null, TestTokens.system()).status()).isEqualTo(200);
      assertThat(call("POST", "/api/inventory/reservations/expire", null, TestTokens.buyer(UUID.randomUUID())).status()).isEqualTo(403);
   }

   // ----------------------------------------------------------- reservations

   @Test
   void reservingManyItemsIsAllOrNothing() throws Exception {
      UUID sellerId = UUID.randomUUID();
      long plenty = newSkuWithStock(sellerId, 10);
      long scarce = newSkuWithStock(sellerId, 1);

      Reply failed = call("POST", "/api/inventory/reservations", reserveBody("chk-fail", 60, plenty, 3, scarce, 5));

      assertThat(failed.status()).isIn(409, 422);
      assertThat(reserved(plenty)).as("the first line was rolled back").isZero();
      assertThat(reserved(scarce)).isZero();
      assertThat(jdbc.queryForObject("select count(*) from inventory_reservations where reference_id = 'chk-fail'", Integer.class)).isZero();
   }

   @Test
   void reservingIsIdempotentForTheSameReference() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      String reference = "chk-" + UUID.randomUUID();

      Reply first = call("POST", "/api/inventory/reservations", reserveBody(reference, 60, sku, 4));
      Reply second = call("POST", "/api/inventory/reservations", reserveBody(reference, 60, sku, 4));

      assertThat(first.status()).isEqualTo(201);
      assertThat(second.status()).isEqualTo(201);
      assertThat(second.body().path("reservations").get(0).path("reservationId").asText())
         .isEqualTo(first.body().path("reservations").get(0).path("reservationId").asText());
      assertThat(reserved(sku)).as("reserved once, not twice").isEqualTo(4);
      assertThat(jdbc.queryForObject("select count(*) from outbox_events where payload like ?", Integer.class,
         "%" + reference + "%")).as("replays emit no events").isEqualTo(1);
   }

   @Test
   void reservationInputsAreBounded() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);

      assertThat(call("POST", "/api/inventory/reservations", reserveBody("r1", Long.MAX_VALUE, sku, 1)).status()).isEqualTo(400);
      assertThat(call("POST", "/api/inventory/reservations", reserveBody("r2", 0, sku, 1)).status()).isEqualTo(400);
      assertThat(call("POST", "/api/inventory/reservations", reserveBody("r3", 60, sku, 100_000)).status()).isEqualTo(400);
      assertThat(call("POST", "/api/inventory/reservations", reserveBody("r4", 60)).status()).isEqualTo(400);
   }

   @Test
   void commitConsumesStockAndIsIdempotent() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 60, sku, 4));
      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();
      String body = "{\"reservationIds\":[\"" + id + "\"]}";

      assertThat(call("POST", "/api/inventory/reservations/commit", body).status()).isEqualTo(200);
      assertThat(call("POST", "/api/inventory/reservations/commit", body).status()).as("retry is safe").isEqualTo(200);

      assertThat(total(sku)).isEqualTo(6);
      assertThat(reserved(sku)).isZero();
      assertThat(call("POST", "/api/inventory/reservations/release", body).status())
         .as("a committed reservation cannot be released").isIn(409, 422);
   }

   @Test
   void releaseReturnsStockAndIsIdempotent() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 60, sku, 4));
      String body = "{\"reservationIds\":[\"" + reserved.body().path("reservations").get(0).path("reservationId").asText() + "\"]}";

      assertThat(call("POST", "/api/inventory/reservations/release", body).status()).isEqualTo(200);
      assertThat(call("POST", "/api/inventory/reservations/release", body).status()).isEqualTo(200);

      assertThat(reserved(sku)).isZero();
      assertThat(total(sku)).isEqualTo(10);
   }

   @Test
   void aBatchCommitFailsAsAWholeWhenOneReservationIsUnknown() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 60, sku, 4));
      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();

      Reply commit = call("POST", "/api/inventory/reservations/commit",
         "{\"reservationIds\":[\"" + id + "\",\"" + UUID.randomUUID() + "\"]}");

      assertThat(commit.status()).isEqualTo(404);
      assertThat(reserved(sku)).as("nothing was committed").isEqualTo(4);
      assertThat(total(sku)).isEqualTo(10);
   }

   @Test
   void overdueReservationsAreExpiredAndTheirStockReturned() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 1, sku, 4));
      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();
      assertThat(reserved(sku)).isEqualTo(4);

      var result = expireReservations.execute(new ExpireReservationsCmd(Instant.now().plusSeconds(120), null));

      assertThat(result.expiredReservationIds()).contains(UUID.fromString(id));
      assertThat(reserved(sku)).isZero();
      assertThat(call("POST", "/api/inventory/reservations/commit", "{\"reservationIds\":[\"" + id + "\"]}").status())
         .as("an expired reservation can no longer be committed").isIn(409, 410, 422);
   }

   @Test
   void anExpiredButNotYetSweptReservationCannotBeCommittedAndChangesNothing() throws Exception {
      long sku = newSkuWithStock(UUID.randomUUID(), 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 1, sku, 4));
      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();
      jdbc.update("update inventory_reservations set expires_at = ? where id = ?",
         java.sql.Timestamp.from(Instant.now().minusSeconds(60)), UUID.fromString(id));

      Reply commit = call("POST", "/api/inventory/reservations/commit", "{\"reservationIds\":[\"" + id + "\"]}");

      assertThat(commit.status()).isIn(409, 410, 422);
      assertThat(total(sku)).isEqualTo(10);
      assertThat(reserved(sku)).isEqualTo(4);
   }

   @Test
   void deletingASkuReleasesItsPendingReservationsAndRemovesTheItem() throws Exception {
      UUID sellerId = UUID.randomUUID();
      long sku = newSkuWithStock(sellerId, 10);
      Reply reserved = call("POST", "/api/inventory/reservations", reserveBody("chk-" + UUID.randomUUID(), 600, sku, 4));
      String id = reserved.body().path("reservations").get(0).path("reservationId").asText();

      inboxListener.listen("del-" + UUID.randomUUID(), "sku.deleted",
         "{\"skuId\":\"" + sku + "\",\"productId\":\"" + UUID.randomUUID() + "\"}");

      assertThat(jdbc.queryForObject("select count(*) from inventory_items where sku_id = ?", Integer.class, sku)).isZero();
      assertThat(jdbc.queryForObject("select status from inventory_reservations where id = ?", String.class, UUID.fromString(id)))
         .isEqualTo("RELEASED");
   }
}
