package com.ecom.order;

import com.ecom.inbox.InboxListener;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of order against a real (H2) database: checkout events build orders through the inbox, and the
 * read endpoints serve them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();

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

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
   }

   private Reply get(String path) throws Exception {
      return call("GET", path);
   }

   /** Calls as an admin, who may see every order; use the three-argument form to act as someone else or anonymously. */
   private Reply call(String method, String path) throws Exception {
      return call(method, path, TestTokens.admin());
   }

   private Reply call(String method, String path, String token) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
         .method(method, HttpRequest.BodyPublishers.noBody());
      if (token != null) {
         request.header("Authorization", "Bearer " + token);
      }
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json);
   }

   private String itemsJson(int quantity) {
      return "[{\"skuId\":\"7\",\"quantity\":" + quantity + ",\"unitPrice\":5.00,\"sellerId\":\"" + UUID.randomUUID() + "\"}]";
   }

   private void drafted(String checkoutId, String userId, long version, int quantity) {
      inboxListener.listen("msg-" + UUID.randomUUID(), "checkout.drafted", "{\"checkoutId\":\"" + checkoutId
         + "\",\"userId\":\"" + userId + "\",\"items\":" + itemsJson(quantity) + ",\"version\":" + version + "}");
   }

   private void committed(String checkoutId, String userId, long version, int quantity) {
      inboxListener.listen("msg-" + UUID.randomUUID(), "checkout.committed", "{\"checkoutId\":\"" + checkoutId
         + "\",\"userId\":\"" + userId + "\",\"items\":" + itemsJson(quantity) + ",\"version\":" + version + "}");
   }

   private void cancelled(String checkoutId, long version) {
      inboxListener.listen("msg-" + UUID.randomUUID(), "checkout.cancelled",
         "{\"checkoutId\":\"" + checkoutId + "\",\"reason\":\"EXPIRED\",\"version\":" + version + "}");
   }

   // ------------------------------------------------------------ who may do what

   @Test
   void aCustomerSeesOnlyTheirOwnOrders() throws Exception {
      UUID customer = UUID.randomUUID();
      String mine = UUID.randomUUID().toString();
      String theirs = UUID.randomUUID().toString();
      drafted(mine, customer.toString(), 0, 1);
      drafted(theirs, UUID.randomUUID().toString(), 0, 1);
      String token = TestTokens.buyer(customer);

      assertThat(call("GET", "/orders/" + mine, null).status()).as("anonymous").isEqualTo(401);
      assertThat(call("GET", "/orders", null).status()).isEqualTo(401);
      assertThat(call("GET", "/orders/" + mine, TestTokens.system()).status()).as("a service is not a customer").isEqualTo(403);
      assertThat(call("GET", "/orders/" + mine, TestTokens.buyer(UUID.randomUUID())).status()).as("someone else").isEqualTo(403);
      assertThat(call("GET", "/orders/" + mine, token).status()).isEqualTo(200);
      assertThat(call("GET", "/orders/" + theirs, token).status()).isEqualTo(403);
      assertThat(call("GET", "/orders/" + theirs).status()).as("admin").isEqualTo(200);

      Reply list = call("GET", "/orders", token);
      assertThat(list.status()).isEqualTo(200);
      assertThat(list.body().path("totalElements").asInt()).as("no customerId means the caller's").isEqualTo(1);
      assertThat(list.body().path("items").get(0).path("orderId").asText()).isEqualTo(mine);
      assertThat(call("GET", "/orders?customerId=" + customer, token).body().path("totalElements").asInt()).isEqualTo(1);
      assertThat(call("GET", "/orders?customerId=" + UUID.randomUUID(), token).status()).as("another customer's list").isEqualTo(403);
   }

   // ------------------------------------------------------------------ events

   @Test
   void draftedThenCommittedBuildsACommittedOrder() throws Exception {
      String id = UUID.randomUUID().toString();
      String user = UUID.randomUUID().toString();
      drafted(id, user, 0, 2);

      Reply draft = get("/orders/" + id);
      assertThat(draft.status()).isEqualTo(200);
      assertThat(draft.body().path("status").asText()).isEqualTo("DRAFT");
      assertThat(draft.body().path("customerId").asText()).isEqualTo(user);
      assertThat(draft.body().path("totalAmount").decimalValue()).isEqualByComparingTo("10.00");
      assertThat(draft.body().path("items")).hasSize(1);

      committed(id, user, 1, 2);

      Reply done = get("/orders/" + id);
      assertThat(done.body().path("status").asText()).isEqualTo("COMMITTED");
      assertThat(done.body().path("version").asLong()).isEqualTo(1);
      assertThat(done.body().path("items")).hasSize(1);
      assertThat(jdbc.queryForObject("select count(*) from order_items where order_id = ?", Integer.class, UUID.fromString(id))).isEqualTo(1);
   }

   @Test
   void duplicateMessagesAndReorderedEventsEndInTheTerminalState() throws Exception {
      String id = UUID.randomUUID().toString();
      String user = UUID.randomUUID().toString();

      committed(id, user, 1, 2);
      inboxListener.listen("msg-same", "checkout.committed", "{\"checkoutId\":\"" + id + "\",\"userId\":\"" + user
         + "\",\"items\":" + itemsJson(2) + ",\"version\":1}");
      inboxListener.listen("msg-same", "checkout.committed", "{\"checkoutId\":\"" + id + "\",\"userId\":\"" + user
         + "\",\"items\":" + itemsJson(2) + ",\"version\":1}");
      drafted(id, user, 0, 5); // the late drafted
      cancelled(id, 2);

      Reply order = get("/orders/" + id);
      assertThat(order.body().path("status").asText()).isEqualTo("COMMITTED");
      assertThat(order.body().path("items")).hasSize(1);
      assertThat(order.body().path("items").get(0).path("quantity").asInt()).isEqualTo(2);
      assertThat(jdbc.queryForObject("select count(*) from orders where id = ?", Integer.class, UUID.fromString(id))).isEqualTo(1);
   }

   @Test
   void cancelledBeforeDraftedIsCreatedCancelledAndFilledInLater() throws Exception {
      String id = UUID.randomUUID().toString();
      String user = UUID.randomUUID().toString();

      cancelled(id, 1);
      Reply first = get("/orders/" + id);
      assertThat(first.body().path("status").asText()).isEqualTo("CANCELLED");
      assertThat(first.body().path("customerId").isNull()).isTrue();
      assertThat(first.body().path("items")).isEmpty();

      drafted(id, user, 0, 3);

      Reply filled = get("/orders/" + id);
      assertThat(filled.body().path("status").asText()).isEqualTo("CANCELLED");
      assertThat(filled.body().path("customerId").asText()).isEqualTo(user);
      assertThat(filled.body().path("items")).hasSize(1);
   }

   // ------------------------------------------------------------------ reads

   @Test
   void unknownOrderIs404() throws Exception {
      assertThat(get("/orders/" + UUID.randomUUID()).status()).isEqualTo(404);
   }

   @Test
   void malformedOrderIdIs400() throws Exception {
      assertThat(get("/orders/not-a-uuid").status()).isEqualTo(400);
   }

   @Test
   void listFiltersByCustomerAndStatusAndPagesNewestFirst() throws Exception {
      String user = UUID.randomUUID().toString();
      String[] ids = new String[5];
      for (int i = 0; i < ids.length; i++) {
         ids[i] = UUID.randomUUID().toString();
         drafted(ids[i], user, 0, 1);
         Thread.sleep(5); // distinct creation instants
      }
      committed(ids[1], user, 1, 1);
      drafted(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 0, 1); // someone else's

      Reply all = get("/orders?customerId=" + user + "&size=2");
      assertThat(all.status()).isEqualTo(200);
      assertThat(all.body().path("totalElements").asInt()).isEqualTo(5);
      assertThat(all.body().path("totalPages").asInt()).isEqualTo(3);
      assertThat(all.body().path("items")).hasSize(2);
      assertThat(all.body().path("items").get(0).path("orderId").asText()).isEqualTo(ids[4]);
      assertThat(all.body().path("items").get(1).path("orderId").asText()).isEqualTo(ids[3]);

      Reply last = get("/orders?customerId=" + user + "&size=2&page=2");
      assertThat(last.body().path("items")).hasSize(1);
      assertThat(last.body().path("items").get(0).path("orderId").asText()).isEqualTo(ids[0]);

      Reply onlyCommitted = get("/orders?customerId=" + user + "&status=COMMITTED");
      assertThat(onlyCommitted.body().path("totalElements").asInt()).isEqualTo(1);
      assertThat(onlyCommitted.body().path("items").get(0).path("orderId").asText()).isEqualTo(ids[1]);

      Reply nobody = get("/orders?customerId=" + UUID.randomUUID());
      assertThat(nobody.status()).isEqualTo(200);
      assertThat(nobody.body().path("items")).isEmpty();
   }

   @Test
   void badListParametersAre400() throws Exception {
      assertThat(get("/orders?size=101").status()).isEqualTo(400);
      assertThat(get("/orders?size=0").status()).isEqualTo(400);
      assertThat(get("/orders?page=-1").status()).isEqualTo(400);
      assertThat(get("/orders?status=SHIPPED").status()).isEqualTo(400);
      assertThat(get("/orders?customerId=nope").status()).isEqualTo(400);
      assertThat(get("/orders?size=100").status()).isEqualTo(200);
   }

   @Test
   void thereAreNoWriteEndpoints() throws Exception {
      assertThat(call("POST", "/orders").status()).isEqualTo(405);
      assertThat(call("DELETE", "/orders/" + UUID.randomUUID()).status()).isEqualTo(405);
   }
}
