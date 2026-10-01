package com.ecom.inventory;

import com.ecom.inbox.InboxListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP-level tests of the read endpoints against a real (H2) database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryReadsIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final AtomicLong SKU_IDS = new AtomicLong(50_000);

   @DynamicPropertySource
   static void jwt(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   InboxListener inboxListener;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
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

   private Reply get(String path) throws Exception {
      return call("GET", path, null);
   }

   private long newSkuWithStock(int quantity) throws Exception {
      long skuId = SKU_IDS.incrementAndGet();
      inboxListener.listen("msg-" + UUID.randomUUID(), "sku.created", "{\"skuId\":\"" + skuId + "\",\"productId\":\""
         + UUID.randomUUID() + "\",\"sellerId\":\"" + UUID.randomUUID() + "\",\"price\":10.00}");
      if (quantity > 0) {
         assertThat(call("POST", "/api/inventory/" + skuId + "/stock/add", "{\"quantity\":" + quantity + "}").status()).isEqualTo(200);
      }
      return skuId;
   }

   private Reply reserve(String reference, long skuA, int qtyA, long skuB, int qtyB) throws Exception {
      Reply reply = call("POST", "/api/inventory/reservations", "{\"referenceId\":\"" + reference + "\",\"ttlSeconds\":600,\"items\":["
         + "{\"skuId\":" + skuA + ",\"quantity\":" + qtyA + "},{\"skuId\":" + skuB + ",\"quantity\":" + qtyB + "}]}");
      assertThat(reply.status()).isEqualTo(201);
      return reply;
   }

   // ------------------------------------------------------------------ stock

   @Test
   void getsTheStockOfOneSku() throws Exception {
      long sku = newSkuWithStock(10);
      long other = newSkuWithStock(5);
      reserve("chk-" + UUID.randomUUID(), sku, 4, other, 1);

      Reply reply = get("/api/inventory/" + sku);

      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body().path("skuId").asLong()).isEqualTo(sku);
      assertThat(reply.body().path("totalQuantity").asInt()).isEqualTo(10);
      assertThat(reply.body().path("reservedQuantity").asInt()).isEqualTo(4);
      assertThat(reply.body().path("availableQuantity").asInt()).isEqualTo(6);
   }

   @Test
   void anUnknownSkuIs404AndABadSkuIs400() throws Exception {
      assertThat(get("/api/inventory/9999999").status()).isEqualTo(404);
      assertThat(get("/api/inventory/abc").status()).isEqualTo(400);
   }

   @Test
   void bulkLookupReturnsOnlyTheSkusThatExist() throws Exception {
      long a = newSkuWithStock(3);
      long b = newSkuWithStock(0);

      Reply reply = get("/api/inventory?skuIds=" + b + "," + a + ",9999998," + a);

      assertThat(reply.status()).isEqualTo(200);
      JsonNode items = reply.body().path("items");
      assertThat(items).hasSize(2);
      assertThat(items.get(0).path("skuId").asLong()).isEqualTo(a);
      assertThat(items.get(0).path("availableQuantity").asInt()).isEqualTo(3);
      assertThat(items.get(1).path("skuId").asLong()).isEqualTo(b);
      assertThat(items.get(1).path("totalQuantity").asInt()).isZero();
   }

   @Test
   void bulkLookupAcceptsExactlyOneHundredIdsAndRejectsMore() throws Exception {
      long sku = newSkuWithStock(1);
      StringBuilder hundred = new StringBuilder(String.valueOf(sku));
      for (int i = 1; i < 100; i++) {
         hundred.append(',').append(8_000_000 + i);
      }

      Reply ok = get("/api/inventory?skuIds=" + hundred);
      assertThat(ok.status()).isEqualTo(200);
      assertThat(ok.body().path("items")).hasSize(1);

      assertThat(get("/api/inventory?skuIds=" + hundred + ",8000100").status()).isEqualTo(400);
   }

   @Test
   void bulkLookupRejectsEmptyAndNonNumericIds() throws Exception {
      assertThat(get("/api/inventory?skuIds=").status()).isEqualTo(400);
      assertThat(get("/api/inventory?skuIds=1,two").status()).isEqualTo(400);
   }

   @Test
   void listingIsPagedAndSortedBySkuId() throws Exception {
      long first = newSkuWithStock(1);
      long second = newSkuWithStock(2);
      long third = newSkuWithStock(3);

      Reply all = get("/api/inventory?page=0&size=100");
      assertThat(all.status()).isEqualTo(200);
      List<Long> skuIds = new ArrayList<>();
      all.body().path("items").forEach(i -> skuIds.add(i.path("skuId").asLong()));
      assertThat(skuIds).isSorted().contains(first, second, third);
      long total = all.body().path("totalElements").asLong();
      assertThat(total).isEqualTo(skuIds.size());
      assertThat(all.body().path("size").asInt()).isEqualTo(100);

      Reply firstPage = get("/api/inventory?page=0&size=2");
      assertThat(firstPage.body().path("items")).hasSize(2);
      assertThat(firstPage.body().path("items").get(0).path("skuId").asLong()).isEqualTo(skuIds.get(0));
      assertThat(firstPage.body().path("items").get(1).path("skuId").asLong()).isEqualTo(skuIds.get(1));
      assertThat(firstPage.body().path("totalElements").asLong()).isEqualTo(total);
      assertThat(firstPage.body().path("totalPages").asInt()).isEqualTo((int) ((total + 1) / 2));

      Reply secondPage = get("/api/inventory?page=1&size=2");
      assertThat(secondPage.body().path("page").asInt()).isEqualTo(1);
      assertThat(secondPage.body().path("items").get(0).path("skuId").asLong()).isEqualTo(skuIds.get(2));

      Reply beyond = get("/api/inventory?page=100000&size=2");
      assertThat(beyond.status()).isEqualTo(200);
      assertThat(beyond.body().path("items")).isEmpty();
   }

   @Test
   void listingUsesDefaultsAndRejectsBadPaging() throws Exception {
      newSkuWithStock(1);

      Reply defaults = get("/api/inventory");
      assertThat(defaults.status()).isEqualTo(200);
      assertThat(defaults.body().path("page").asInt()).isZero();
      assertThat(defaults.body().path("size").asInt()).isEqualTo(20);

      assertThat(get("/api/inventory?size=100").status()).isEqualTo(200);
      assertThat(get("/api/inventory?size=101").status()).isEqualTo(400);
      assertThat(get("/api/inventory?size=0").status()).isEqualTo(400);
      assertThat(get("/api/inventory?page=-1").status()).isEqualTo(400);
      assertThat(get("/api/inventory?page=x").status()).isEqualTo(400);
   }

   // ----------------------------------------------------------- reservations

   @Test
   void getsAReservationById() throws Exception {
      long a = newSkuWithStock(10);
      long b = newSkuWithStock(10);
      Reply created = reserve("chk-" + UUID.randomUUID(), a, 2, b, 3);
      JsonNode line = created.body().path("reservations").get(0);

      Reply reply = get("/api/inventory/reservations/" + line.path("reservationId").asText());

      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body().path("reservationId").asText()).isEqualTo(line.path("reservationId").asText());
      assertThat(reply.body().path("referenceId").asText()).isEqualTo(line.path("referenceId").asText());
      assertThat(reply.body().path("skuId").asLong()).isEqualTo(line.path("skuId").asLong());
      assertThat(reply.body().path("quantity").asInt()).isEqualTo(line.path("quantity").asInt());
      assertThat(reply.body().path("status").asText()).isEqualTo("PENDING");
      assertThat(reply.body().path("expiresAt").asText()).isNotBlank();
   }

   @Test
   void anUnknownReservationIs404AndAMalformedIdIs400() throws Exception {
      assertThat(get("/api/inventory/reservations/" + UUID.randomUUID()).status()).isEqualTo(404);
      assertThat(get("/api/inventory/reservations/not-a-uuid").status()).isEqualTo(400);
   }

   @Test
   void listsTheReservationsOfACheckoutWithAnOptionalStatusFilter() throws Exception {
      long a = newSkuWithStock(10);
      long b = newSkuWithStock(10);
      String checkout = "chk-" + UUID.randomUUID();
      Reply created = reserve(checkout, b, 3, a, 2);
      reserve("chk-" + UUID.randomUUID(), a, 1, b, 1);
      String releasedId = created.body().path("reservations").get(0).path("reservationId").asText();
      String releasedSku = created.body().path("reservations").get(0).path("skuId").asText();
      call("POST", "/api/inventory/reservations/release", "{\"reservationIds\":[\"" + releasedId + "\"]}");

      Reply everything = get("/api/inventory/reservations?checkoutId=" + checkout);
      assertThat(everything.status()).isEqualTo(200);
      JsonNode lines = everything.body().path("reservations");
      assertThat(lines).hasSize(2);
      assertThat(lines.get(0).path("skuId").asLong()).isLessThan(lines.get(1).path("skuId").asLong());
      lines.forEach(l -> assertThat(l.path("referenceId").asText()).isEqualTo(checkout));

      Reply released = get("/api/inventory/reservations?checkoutId=" + checkout + "&status=RELEASED");
      assertThat(released.body().path("reservations")).hasSize(1);
      assertThat(released.body().path("reservations").get(0).path("skuId").asText()).isEqualTo(releasedSku);
      assertThat(released.body().path("reservations").get(0).path("status").asText()).isEqualTo("RELEASED");

      Reply pending = get("/api/inventory/reservations?checkoutId=" + checkout + "&status=PENDING");
      assertThat(pending.body().path("reservations")).hasSize(1);
      assertThat(pending.body().path("reservations").get(0).path("status").asText()).isEqualTo("PENDING");

      assertThat(get("/api/inventory/reservations?checkoutId=" + checkout + "&status=COMMITTED").body().path("reservations")).isEmpty();
   }

   @Test
   void aCheckoutWithoutReservationsGivesAnEmptyList() throws Exception {
      Reply reply = get("/api/inventory/reservations?checkoutId=chk-" + UUID.randomUUID());

      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body().path("reservations")).isEmpty();
   }

   @Test
   void listingReservationsRejectsMissingCheckoutAndUnknownStatus() throws Exception {
      assertThat(get("/api/inventory/reservations").status()).isEqualTo(400);
      assertThat(get("/api/inventory/reservations?checkoutId=").status()).isEqualTo(400);
      assertThat(get("/api/inventory/reservations?checkoutId=" + "x".repeat(101)).status()).isEqualTo(400);
      assertThat(get("/api/inventory/reservations?checkoutId=chk-1&status=BOGUS").status()).isEqualTo(400);
   }

   @Test
   void theReservationRoutesAreNotSwallowedByTheSkuRoute() throws Exception {
      // GET /inventory/reservations must reach the reservation listing (400 for the missing checkoutId),
      // not the sku lookup (which would answer 400 for a non-numeric sku id too, so check the body)
      Reply reply = get("/api/inventory/reservations");
      assertThat(reply.status()).isEqualTo(400);
      assertThat(reply.body().path("invalidFields").has("checkoutId")).isTrue();
   }
}
