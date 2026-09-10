package com.ecom.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests of the listing read endpoints. Lists are always narrowed to a fresh seller so the tests do not see
 * each other's products in the shared in-memory database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ListingReadIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();

   @DynamicPropertySource
   static void jwt(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
   }

   @Value("${local.server.port}")
   int port;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
      String text(String field) {
         return body.path(field).asText();
      }
   }

   /** Calls as an admin, who may act on anything; use the four-argument form to act as someone else or anonymously. */
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

   /** Reads are public, so they are made without a token. */
   private Reply get(String path) throws Exception {
      return call("GET", path, null, null);
   }

   private Reply createProduct(UUID seller, String title) throws Exception {
      return call("POST", "/products",
         "{\"title\":\"" + title + "\",\"description\":\"A product\",\"defaultPrice\":100.00}", TestTokens.seller(seller));
   }

   private Reply createSku(String productId, String propertiesJson) throws Exception {
      return call("POST", "/products/" + productId + "/skus",
         "{\"price\":25.50,\"properties\":" + propertiesJson + "}");
   }

   private static List<String> titles(Reply page) {
      List<String> titles = new ArrayList<>();
      page.body().path("items").forEach(item -> titles.add(item.path("title").asText()));
      return titles;
   }

   // ---------------------------------------------------------------- GET /products/{id}

   @Test
   void aProductCanBeReadById() throws Exception {
      UUID seller = UUID.randomUUID();
      Reply created = createProduct(seller, "Laptop Pro");

      Reply read = get("/products/" + created.text("id"));

      assertThat(read.status()).isEqualTo(200);
      assertThat(read.text("id")).isEqualTo(created.text("id"));
      assertThat(read.text("title")).isEqualTo("Laptop Pro");
      assertThat(read.text("sellerId")).isEqualTo(seller.toString());
      assertThat(read.body().path("version").asLong()).isZero();
   }

   @Test
   void anUnknownProductIsNotFound() throws Exception {
      Reply read = get("/products/" + UUID.randomUUID());

      assertThat(read.status()).isEqualTo(404);
      assertThat(read.text("errorCode")).isEqualTo("RESOURCE_NOT_FOUND");
   }

   @Test
   void aMalformedProductIdIsABadRequest() throws Exception {
      assertThat(get("/products/not-a-uuid").status()).isEqualTo(400);
   }

   @Test
   void aSoftDeletedProductIsNotFoundAndNeverListed() throws Exception {
      UUID seller = UUID.randomUUID();
      String keep = createProduct(seller, "Kept product").text("id");
      String gone = createProduct(seller, "Gone product").text("id");
      call("DELETE", "/products/" + gone + "?version=0", null);

      assertThat(get("/products/" + gone).status()).isEqualTo(404);
      assertThat(get("/products/" + keep).status()).isEqualTo(200);
      Reply page = get("/products?sellerId=" + seller);
      assertThat(titles(page)).containsExactly("Kept product");
      assertThat(page.body().path("totalElements").asLong()).isEqualTo(1);
   }

   // ---------------------------------------------------------------- GET /products

   @Test
   void productsArePagedWithTheNextPageAdvertised() throws Exception {
      UUID seller = UUID.randomUUID();
      for (String title : List.of("Product A", "Product B", "Product C", "Product D", "Product E")) {
         createProduct(seller, title);
      }

      Reply first = get("/products?sellerId=" + seller + "&size=2&sort=title,asc");
      Reply second = get("/products?sellerId=" + seller + "&size=2&page=1&sort=title,asc");
      Reply last = get("/products?sellerId=" + seller + "&size=2&page=2&sort=title,asc");
      Reply beyond = get("/products?sellerId=" + seller + "&size=2&page=3&sort=title,asc");

      assertThat(titles(first)).containsExactly("Product A", "Product B");
      assertThat(titles(second)).containsExactly("Product C", "Product D");
      assertThat(titles(last)).containsExactly("Product E");
      assertThat(titles(beyond)).isEmpty();
      assertThat(first.body().path("totalElements").asLong()).isEqualTo(5);
      assertThat(first.body().path("totalPages").asInt()).isEqualTo(3);
      assertThat(first.body().path("page").asInt()).isZero();
      assertThat(first.body().path("size").asInt()).isEqualTo(2);
      assertThat(first.body().path("nextPage").asInt()).isEqualTo(1);
      assertThat(last.body().path("nextPage").isNull()).isTrue();
   }

   @Test
   void productsCanBeSortedByTitleAndCreationTime() throws Exception {
      UUID seller = UUID.randomUUID();
      createProduct(seller, "Banana split");
      createProduct(seller, "Cherry pie");
      createProduct(seller, "Apple tart");

      assertThat(titles(get("/products?sellerId=" + seller + "&sort=title,asc")))
         .containsExactly("Apple tart", "Banana split", "Cherry pie");
      assertThat(titles(get("/products?sellerId=" + seller + "&sort=title,desc")))
         .containsExactly("Cherry pie", "Banana split", "Apple tart");
      assertThat(titles(get("/products?sellerId=" + seller + "&sort=createdAt,asc")))
         .containsExactly("Banana split", "Cherry pie", "Apple tart");
      // newest first is the default
      assertThat(titles(get("/products?sellerId=" + seller)))
         .containsExactly("Apple tart", "Cherry pie", "Banana split");
      assertThat(titles(get("/products?sellerId=" + seller + "&sort=createdAt,desc")))
         .containsExactly("Apple tart", "Cherry pie", "Banana split");
   }

   @Test
   void productsCanBeFilteredBySellerAndByTitle() throws Exception {
      UUID seller = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      createProduct(seller, "Gaming Laptop");
      createProduct(seller, "Office LAPTOP");
      createProduct(seller, "Desk lamp");
      createProduct(other, "Other Laptop");

      assertThat(titles(get("/products?sellerId=" + seller + "&sort=title,asc")))
         .containsExactly("Desk lamp", "Gaming Laptop", "Office LAPTOP");
      assertThat(titles(get("/products?sellerId=" + seller + "&title=laptop&sort=title,asc")))
         .containsExactly("Gaming Laptop", "Office LAPTOP");
      assertThat(titles(get("/products?sellerId=" + other + "&title=LAPTOP"))).containsExactly("Other Laptop");
      assertThat(titles(get("/products?sellerId=" + seller + "&title=nothing"))).isEmpty();
   }

   @Test
   void theTitleFilterTreatsWildcardsAsPlainText() throws Exception {
      UUID seller = UUID.randomUUID();
      createProduct(seller, "100% cotton");
      createProduct(seller, "Plain shirt");

      assertThat(titles(get("/products?sellerId=" + seller + "&title=%25"))).containsExactly("100% cotton");
      assertThat(titles(get("/products?sellerId=" + seller + "&title=_"))).isEmpty();
   }

   @Test
   void invalidPagingAndSortParametersAreBadRequests() throws Exception {
      assertThat(get("/products?page=-1").status()).isEqualTo(400);
      assertThat(get("/products?size=0").status()).isEqualTo(400);
      assertThat(get("/products?size=101").status()).isEqualTo(400);
      assertThat(get("/products?page=abc").status()).isEqualTo(400);
      assertThat(get("/products?size=abc").status()).isEqualTo(400);
      assertThat(get("/products?sellerId=not-a-uuid").status()).isEqualTo(400);
      assertThat(get("/products?sort=price,asc").status()).isEqualTo(400);
      assertThat(get("/products?sort=title,sideways").status()).isEqualTo(400);
   }

   @Test
   void theMaximumPageSizeIsAccepted() throws Exception {
      assertThat(get("/products?size=100").status()).isEqualTo(200);
   }

   // ---------------------------------------------------------------- skus

   @Test
   void theSkusOfAProductAreListedWithoutDeletedOnes() throws Exception {
      String productId = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");
      long red = createSku(productId, "{\"color\":\"red\"}").body().path("skuId").asLong();
      Reply blue = createSku(productId, "{\"color\":\"blue\"}");
      long green = createSku(productId, "{\"color\":\"green\"}").body().path("skuId").asLong();
      call("DELETE", "/products/" + productId + "/skus/" + green + "?version=0", null);

      Reply list = get("/products/" + productId + "/skus");

      assertThat(list.status()).isEqualTo(200);
      assertThat(list.body().isArray()).isTrue();
      assertThat(list.body()).hasSize(2);
      assertThat(list.body().get(0).path("skuId").asLong()).isEqualTo(red);
      assertThat(list.body().get(1).path("skuId").asLong()).isEqualTo(blue.body().path("skuId").asLong());
      assertThat(list.body().get(1).path("properties").path("color").asText()).isEqualTo("blue");
      assertThat(list.body().get(0).path("productId").asText()).isEqualTo(productId);
   }

   @Test
   void aProductWithoutSkusListsAnEmptyArray() throws Exception {
      String productId = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");

      Reply list = get("/products/" + productId + "/skus");

      assertThat(list.status()).isEqualTo(200);
      assertThat(list.body().isArray()).isTrue();
      assertThat(list.body()).isEmpty();
   }

   @Test
   void theSkusOfAnUnknownOrDeletedProductAreNotFound() throws Exception {
      assertThat(get("/products/" + UUID.randomUUID() + "/skus").status()).isEqualTo(404);

      String productId = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");
      createSku(productId, "{\"color\":\"red\"}");
      call("DELETE", "/products/" + productId + "?version=0", null);

      assertThat(get("/products/" + productId + "/skus").status()).isEqualTo(404);
   }

   @Test
   void aSkuCanBeReadThroughItsProduct() throws Exception {
      String productId = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");
      Reply created = createSku(productId, "{\"color\":\"red\"}");
      String skuId = created.text("skuId");

      Reply read = get("/products/" + productId + "/skus/" + skuId);

      assertThat(read.status()).isEqualTo(200);
      assertThat(read.text("skuId")).isEqualTo(skuId);
      assertThat(read.text("productId")).isEqualTo(productId);
      assertThat(read.body().path("properties").path("color").asText()).isEqualTo("red");
      assertThat(read.body().path("price").decimalValue()).isEqualByComparingTo("25.50");
      assertThat(read.body().path("version").asLong()).isZero();
   }

   @Test
   void aSkuIsNotFoundThroughAnotherProductOrWhenDeletedOrUnknown() throws Exception {
      UUID seller = UUID.randomUUID();
      String productId = createProduct(seller, "Laptop Pro").text("id");
      String otherId = createProduct(seller, "Phone Max").text("id");
      long skuId = createSku(productId, "{\"color\":\"red\"}").body().path("skuId").asLong();
      long deleted = createSku(productId, "{\"color\":\"blue\"}").body().path("skuId").asLong();
      call("DELETE", "/products/" + productId + "/skus/" + deleted + "?version=0", null);

      assertThat(get("/products/" + otherId + "/skus/" + skuId).status()).isEqualTo(404);
      assertThat(get("/products/" + productId + "/skus/" + deleted).status()).isEqualTo(404);
      assertThat(get("/products/" + productId + "/skus/999999").status()).isEqualTo(404);
      assertThat(get("/products/" + UUID.randomUUID() + "/skus/" + skuId).status()).isEqualTo(404);
      assertThat(get("/products/" + productId + "/skus/" + skuId).status()).isEqualTo(200);
   }

   @Test
   void aMalformedSkuIdIsABadRequest() throws Exception {
      String productId = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");

      assertThat(get("/products/" + productId + "/skus/abc").status()).isEqualTo(400);
   }

   // ------------------------------------------------------------ lookup by sku id

   @Test
   void skusCanBeLookedUpByIdAloneAndUnknownOrDeletedOnesAreLeftOut() throws Exception {
      String first = createProduct(UUID.randomUUID(), "Laptop Pro").text("id");
      String second = createProduct(UUID.randomUUID(), "Phone Max").text("id");
      long a = createSku(first, "{\"color\":\"red\"}").body().path("skuId").asLong();
      long b = createSku(second, "{\"color\":\"blue\"}").body().path("skuId").asLong();
      long gone = createSku(first, "{\"color\":\"green\"}").body().path("skuId").asLong();
      call("DELETE", "/products/" + first + "/skus/" + gone + "?version=0", null);

      Reply reply = call("GET", "/skus?skuIds=" + b + "," + a + "," + gone + ",9999999," + a, null, TestTokens.system());

      assertThat(reply.status()).isEqualTo(200);
      assertThat(reply.body()).hasSize(2);
      assertThat(reply.body().get(0).path("skuId").asLong()).isEqualTo(Math.min(a, b));
      assertThat(reply.body().get(0).path("price").decimalValue()).isEqualByComparingTo("25.50");
      assertThat(reply.body().get(0).path("sellerId").asText()).isNotBlank();
      assertThat(reply.body().get(0).path("productId").asText()).isIn(first, second);
   }

   @Test
   void theSkuLookupIsInternal() throws Exception {
      assertThat(get("/skus?skuIds=1").status()).as("anonymous").isEqualTo(401);
      assertThat(call("GET", "/skus?skuIds=1", null, TestTokens.buyer()).status()).as("buyer").isEqualTo(403);
      assertThat(call("GET", "/skus?skuIds=1", null, TestTokens.seller(UUID.randomUUID())).status()).as("seller").isEqualTo(403);
      assertThat(call("GET", "/skus?skuIds=1", null, TestTokens.system()).status()).isEqualTo(200);
      assertThat(call("GET", "/skus?skuIds=1", null, TestTokens.admin()).status()).isEqualTo(200);
   }

   @Test
   void theSkuLookupRejectsMissingEmptyOversizedAndNonNumericIds() throws Exception {
      assertThat(call("GET", "/skus", null, TestTokens.system()).status()).isEqualTo(400);
      assertThat(call("GET", "/skus?skuIds=", null, TestTokens.system()).status()).isEqualTo(400);
      assertThat(call("GET", "/skus?skuIds=1,two", null, TestTokens.system()).status()).isEqualTo(400);
      String hundred = java.util.stream.LongStream.rangeClosed(8_000_000, 8_000_099)
         .mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
      assertThat(call("GET", "/skus?skuIds=" + hundred, null, TestTokens.system()).status()).isEqualTo(200);
      assertThat(call("GET", "/skus?skuIds=" + hundred + ",8000100", null, TestTokens.system()).status()).isEqualTo(400);
   }
}
