package com.ecom.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the listing API against a real (H2) database and the real outbox. Unlike the unit tests these
 * exercise schema creation, constraints, soft delete and error mapping.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ListingIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();

   @Value("${local.server.port}")
   int port;

   @Autowired
   JdbcTemplate jdbc;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
      String text(String field) {
         return body.path(field).asText();
      }
   }

   private Reply call(String method, String path, String body) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
         .header("Content-Type", "application/json");
      request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json);
   }

   private static UUID newSeller() {
      return UUID.randomUUID();
   }

   private Reply createProduct(UUID seller, String title) throws Exception {
      return call("POST", "/products",
         "{\"title\":\"" + title + "\",\"description\":\"A product\",\"defaultPrice\":100.00,\"sellerId\":\"" + seller + "\"}");
   }

   private Reply createSku(String productId, String propertiesJson) throws Exception {
      return call("POST", "/products/" + productId + "/skus",
         "{\"price\":25.50,\"properties\":" + propertiesJson + "}");
   }

   // ---------------------------------------------------------------- products

   @Test
   void aProductIsCreatedForTheGivenSellerAndTheOutboxRecordsIt() throws Exception {
      UUID seller = newSeller();

      Reply created = createProduct(seller, "Laptop Pro");

      assertThat(created.status()).isEqualTo(201);
      assertThat(created.text("sellerId")).isEqualTo(seller.toString());
      assertThat(created.body().path("version").asLong()).isZero();

      Integer events = jdbc.queryForObject(
         "select count(*) from outbox_events where aggregate_type = 'product' and aggregate_id = ?",
         Integer.class, created.text("id"));
      assertThat(events).isEqualTo(1);
   }

   @Test
   void theSellerIsRequired() throws Exception {
      Reply missing = call("POST", "/products", "{\"title\":\"Laptop Pro\",\"description\":\"d\",\"defaultPrice\":10.00}");

      assertThat(missing.status()).isEqualTo(400);
      assertThat(missing.body().path("invalidFields").has("sellerId")).isTrue();
   }

   @Test
   void titleLengthIsEnforcedByTheApiAndByTheDatabaseConstraint() throws Exception {
      UUID seller = newSeller();

      Reply tooShort = createProduct(seller, "abcd");
      assertThat(tooShort.status()).isEqualTo(400);
      assertThat(tooShort.body().path("invalidFields").has("title")).isTrue();

      assertThat(createProduct(seller, "abcde").status()).isEqualTo(201);
      assertThat(createProduct(seller, "A much longer but still valid title").status()).isEqualTo(201);

      // the constraint itself
      assertThat(insertProductRow(seller, "Laptop Pro")).isTrue();
      assertThat(insertProductRow(seller, "abc")).isFalse();
      assertThat(insertProductRow(seller, "x".repeat(51))).isFalse();
   }

   private boolean insertProductRow(UUID sellerId, String title) {
      try {
         jdbc.update("""
            insert into products (id, seller_id, title, is_deleted, created_at, modified_at, version)
            values (?, ?, ?, false, current_timestamp, current_timestamp, 0)
            """, UUID.randomUUID(), sellerId, title);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   @Test
   void staleVersionsAreAConflictNotA500() throws Exception {
      String id = createProduct(newSeller(), "Laptop Pro").text("id");

      Reply stale = call("PUT", "/products/" + id, "{\"version\":99,\"title\":\"Renamed title\"}");

      assertThat(stale.status()).isEqualTo(409);
      assertThat(stale.text("errorCode")).isEqualTo("VERSION_CONFLICT");
   }

   @Test
   void patchDistinguishesAnOmittedDescriptionFromAnExplicitNull() throws Exception {
      Reply created = createProduct(newSeller(), "Laptop Pro");
      String id = created.text("id");

      Reply omitted = call("PATCH", "/products/" + id, "{\"version\":0,\"title\":\"Laptop Pro 2\"}");
      assertThat(omitted.status()).isEqualTo(200);
      assertThat(omitted.text("description")).isEqualTo("A product");

      long version = omitted.body().path("version").asLong();
      Reply cleared = call("PATCH", "/products/" + id,
         "{\"version\":" + version + ",\"title\":\"Laptop Pro 2\",\"description\":null}");
      assertThat(cleared.status()).isEqualTo(200);
      assertThat(cleared.body().path("description").isNull()).isTrue();
   }

   @Test
   void aTooLongDescriptionInAPatchIsRejected() throws Exception {
      String id = createProduct(newSeller(), "Laptop Pro").text("id");

      Reply tooLong = call("PATCH", "/products/" + id, "{\"version\":0,\"description\":\"" + "x".repeat(201) + "\"}");

      assertThat(tooLong.status()).isEqualTo(400);
   }

   @Test
   void frameworkErrorsKeepTheirProperStatus() throws Exception {
      assertThat(call("POST", "/products", "{not json").status()).isEqualTo(400);
      assertThat(call("GET", "/nope", null).status()).isEqualTo(404);
      assertThat(call("PUT", "/products/not-a-uuid", "{\"version\":0,\"title\":\"Valid title\"}").status()).isEqualTo(400);
      assertThat(call("GET", "/products", null).status()).isEqualTo(405);
   }

   // -------------------------------------------------------------------- SKUs

   @Test
   void skuPropertiesRoundTripAndUnsafeCharactersAreRejected() throws Exception {
      String productId = createProduct(newSeller(), "Laptop Pro").text("id");

      Reply ok = createSku(productId, "{\"url\":\"http://x.com/a\",\"color\":\"red\"}");
      assertThat(ok.status()).isEqualTo(201);
      assertThat(ok.body().path("properties").path("url").asText()).isEqualTo("http://x.com/a");
      assertThat(ok.body().path("properties").path("color").asText()).isEqualTo("red");

      assertThat(createSku(productId, "{\"tag\":\"a,b\"}").status()).isEqualTo(400);
      assertThat(createSku(productId, "{\"a:b\":\"x\"}").status()).isEqualTo(400);
   }

   @Test
   void duplicatePropertiesForTheSameProductAreAConflict() throws Exception {
      UUID seller = newSeller();
      String productId = createProduct(seller, "Laptop Pro").text("id");

      assertThat(createSku(productId, "{\"color\":\"red\"}").status()).isEqualTo(201);
      Reply duplicate = createSku(productId, "{\"color\":\"red\"}");
      assertThat(duplicate.status()).isEqualTo(409);
      assertThat(duplicate.text("errorCode")).isEqualTo("DUPLICATE_RESOURCE");

      String otherProduct = createProduct(seller, "Another one").text("id");
      assertThat(createSku(otherProduct, "{\"color\":\"red\"}").status()).isEqualTo(201);
   }

   @Test
   void updatingASkuToAnExistingCombinationIsAConflict() throws Exception {
      String productId = createProduct(newSeller(), "Laptop Pro").text("id");
      createSku(productId, "{\"color\":\"red\"}");
      Reply blue = createSku(productId, "{\"color\":\"blue\"}");

      Reply update = call("PUT", "/products/" + productId + "/skus/" + blue.body().path("skuId").asLong(),
         "{\"version\":" + blue.body().path("version").asLong() + ",\"properties\":{\"add\":{\"color\":\"red\"}}}");

      assertThat(update.status()).isEqualTo(409);
   }

   @Test
   void deletingASkuIsSoftAndFreesItsPropertyCombination() throws Exception {
      String productId = createProduct(newSeller(), "Laptop Pro").text("id");
      Reply sku = createSku(productId, "{\"color\":\"red\"}");
      long skuId = sku.body().path("skuId").asLong();

      Reply deleted = call("DELETE", "/products/" + productId + "/skus/" + skuId + "?version=" + sku.body().path("version").asLong(), null);
      assertThat(deleted.status()).isEqualTo(200);

      Boolean flag = jdbc.queryForObject("select is_deleted from product_skus where id = ?", Boolean.class, skuId);
      assertThat(flag).isTrue();
      assertThat(call("DELETE", "/products/" + productId + "/skus/" + skuId + "?version=1", null).status()).isEqualTo(404);
      assertThat(createSku(productId, "{\"color\":\"red\"}").status()).isEqualTo(201);
   }

   @Test
   void deletingAProductSoftDeletesItsSkusAndEmitsOneEventPerSku() throws Exception {
      Reply product = createProduct(newSeller(), "Laptop Pro");
      String productId = product.text("id");
      long sku1 = createSku(productId, "{\"color\":\"red\"}").body().path("skuId").asLong();
      long sku2 = createSku(productId, "{\"color\":\"blue\"}").body().path("skuId").asLong();

      Reply deleted = call("DELETE", "/products/" + productId + "?version=0", null);

      assertThat(deleted.status()).isEqualTo(200);
      List<Long> deletedSkus = List.of(deleted.body().path("deletedSkuIds").get(0).asLong(),
         deleted.body().path("deletedSkuIds").get(1).asLong());
      assertThat(deletedSkus).containsExactlyInAnyOrder(sku1, sku2);

      assertThat(jdbc.queryForObject("select is_deleted from products where id = ?", Boolean.class, UUID.fromString(productId))).isTrue();
      assertThat(jdbc.queryForObject("select count(*) from product_skus where product_id = ? and is_deleted = false",
         Integer.class, UUID.fromString(productId))).isZero();

      List<String> deleteEvents = jdbc.queryForList(
         "select payload from outbox_events where payload like '%\"action\":\"deleted\"%' "
            + "and (aggregate_id = ? or aggregate_id = ? or aggregate_id = ?) order by created_at",
         String.class, productId, String.valueOf(sku1), String.valueOf(sku2));
      assertThat(deleteEvents).hasSize(3);

      assertThat(call("PUT", "/products/" + productId, "{\"version\":1,\"title\":\"Renamed title\"}").status()).isEqualTo(404);
      assertThat(createSku(productId, "{}").status()).isEqualTo(404);
   }

   @Test
   void skuPriceCannotBeNegative() throws Exception {
      String productId = createProduct(newSeller(), "Laptop Pro").text("id");

      Reply negative = call("POST", "/products/" + productId + "/skus", "{\"price\":-1,\"properties\":{}}");

      assertThat(negative.status()).isEqualTo(400);
   }
}
