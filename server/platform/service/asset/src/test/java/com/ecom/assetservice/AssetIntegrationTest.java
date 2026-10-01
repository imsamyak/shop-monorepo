package com.ecom.assetservice;

import com.ecom.asset.AssetStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The upload API end to end with real access tokens and an in-memory stand-in for the bucket.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AssetIntegrationTest {

   private static final ObjectMapper JSON = new ObjectMapper();
   private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
   private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 4, 5, 6};

   static final Map<String, String> FILES = new ConcurrentHashMap<>();

   @TestConfiguration
   static class InMemoryBucket {
      @Bean
      @Primary
      AssetStorage storage() {
         return new AssetStorage() {
            public void put(String key, byte[] content, String contentType) {
               FILES.put(key, contentType);
            }

            public void delete(String key) {
               FILES.remove(key);
            }

            public String publicUrl(String key) {
               return "https://files.example.com/" + key;
            }
         };
      }
   }

   @DynamicPropertySource
   static void jwt(DynamicPropertyRegistry registry) {
      TestTokens.register(registry);
   }

   @Value("${local.server.port}")
   int port;

   @Autowired
   AssetStorage storage;

   private final HttpClient http = HttpClient.newHttpClient();

   record Reply(int status, JsonNode body) {
   }

   @BeforeEach
   void clear() {
      FILES.clear();
   }

   private record Part(String name, String filename, String contentType, byte[] content) {
   }

   private Reply upload(String token, Part... parts) throws Exception {
      String boundary = "----ecom" + UUID.randomUUID();
      ByteArrayOutputStream body = new ByteArrayOutputStream();
      for (Part part : parts) {
         body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + part.name + "\"; filename=\""
            + part.filename + "\"\r\nContent-Type: " + part.contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
         body.write(part.content);
         body.write("\r\n".getBytes(StandardCharsets.UTF_8));
      }
      body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assets/images"))
         .header("Content-Type", "multipart/form-data; boundary=" + boundary)
         .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
      if (token != null) {
         request.header("Authorization", "Bearer " + token);
      }
      return send(request);
   }

   private Reply delete(String token, String key) throws Exception {
      HttpRequest.Builder request = HttpRequest.newBuilder(
            URI.create("http://localhost:" + port + "/api/assets/images?key=" + key)).DELETE();
      if (token != null) {
         request.header("Authorization", "Bearer " + token);
      }
      return send(request);
   }

   private Reply send(HttpRequest.Builder request) throws Exception {
      HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
      JsonNode json = response.body() == null || response.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(response.body());
      return new Reply(response.statusCode(), json);
   }

   private static Part png() {
      return new Part("files", "a.png", "image/png", PNG);
   }

   private static Part jpeg() {
      return new Part("files", "b.jpg", "image/jpeg", JPEG);
   }

   @Test
   void aSellerUploadsImagesAndGetsTheirAddressesInTheOrderSent() throws Exception {
      UUID user = UUID.randomUUID();

      Reply reply = upload(TestTokens.seller(user), jpeg(), png(), jpeg());

      assertThat(reply.status()).isEqualTo(201);
      JsonNode items = reply.body().path("items");
      assertThat(items).hasSize(3);
      assertThat(items.get(0).path("contentType").asText()).isEqualTo("image/jpeg");
      assertThat(items.get(1).path("contentType").asText()).isEqualTo("image/png");
      assertThat(items.get(2).path("contentType").asText()).isEqualTo("image/jpeg");
      assertThat(items.get(1).path("size").asInt()).isEqualTo(PNG.length);
      for (JsonNode item : items) {
         assertThat(item.path("url").asText()).isEqualTo("https://files.example.com/" + item.path("key").asText());
         assertThat(FILES).containsKey(item.path("key").asText());
      }
      assertThat(items.get(0).path("key").asText()).matches("images/[0-9a-f-]{36}/[0-9a-f-]{36}\\.jpg");
   }

   @Test
   void onlySignedInSellersAndAdminsMayUpload() throws Exception {
      assertThat(upload(null, png()).status()).isEqualTo(401);
      assertThat(upload("not.a.token", png()).status()).isEqualTo(401);
      assertThat(upload(TestTokens.buyer(UUID.randomUUID()), png()).status()).isEqualTo(403);
      assertThat(upload(TestTokens.system(), png()).status()).isEqualTo(403);
      assertThat(FILES).isEmpty();

      assertThat(upload(TestTokens.seller(UUID.randomUUID()), png()).status()).isEqualTo(201);
      assertThat(upload(TestTokens.admin(), png()).status()).isEqualTo(201);
   }

   @Test
   void onlyRealImagesAreAcceptedAndOneBadFileStoresNothing() throws Exception {
      String token = TestTokens.seller(UUID.randomUUID());
      Part text = new Part("files", "evil.png", "image/png", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
      Part svg = new Part("files", "x.svg", "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8));

      assertThat(upload(token, png(), text).status()).isEqualTo(400);
      assertThat(upload(token, svg).status()).isEqualTo(400);
      assertThat(upload(token, new Part("files", "empty.png", "image/png", new byte[0])).status()).isEqualTo(400);
      assertThat(FILES).isEmpty();
   }

   @Test
   void tooManyFilesAndNoFilesAreRejected() throws Exception {
      String token = TestTokens.seller(UUID.randomUUID());
      Part[] eleven = new Part[11];
      for (int i = 0; i < eleven.length; i++) {
         eleven[i] = png();
      }

      assertThat(upload(token, eleven).status()).isEqualTo(400);
      assertThat(upload(token, new Part("other", "a.png", "image/png", PNG)).status()).as("wrong part name").isEqualTo(400);
      assertThat(FILES).isEmpty();
      assertThat(upload(token, java.util.Arrays.copyOf(eleven, 10)).status()).isEqualTo(201);
   }

   @Test
   void anImageOverTheSizeLimitIsRejected() throws Exception {
      byte[] big = new byte[5 * 1024 * 1024 + 1];
      System.arraycopy(PNG, 0, big, 0, PNG.length);

      Reply reply = upload(TestTokens.seller(UUID.randomUUID()), new Part("files", "big.png", "image/png", big));

      assertThat(reply.status()).isIn(400, 413);
      assertThat(FILES).isEmpty();
   }

   @Test
   void anImageCanOnlyBeDeletedByItsOwnerOrAnAdmin() throws Exception {
      UUID owner = UUID.randomUUID();
      String key = upload(TestTokens.sellerUser(owner), png()).body().path("items").get(0).path("key").asText();
      assertThat(key).startsWith("images/" + owner + "/");

      assertThat(delete(null, key).status()).isEqualTo(401);
      assertThat(delete(TestTokens.buyer(owner), key).status()).as("a buyer").isEqualTo(403);
      assertThat(delete(TestTokens.seller(UUID.randomUUID()), key).status()).as("another seller").isEqualTo(403);
      assertThat(FILES).containsKey(key);

      assertThat(delete(TestTokens.sellerUser(owner), key).status()).as("the owner, with a new token").isEqualTo(204);
      assertThat(FILES).doesNotContainKey(key);
   }

   @Test
   void anAdminMayDeleteAnyImageAndDeletingTwiceIsHarmless() throws Exception {
      String key = upload(TestTokens.seller(UUID.randomUUID()), png()).body().path("items").get(0).path("key").asText();
      String admin = TestTokens.admin();

      assertThat(delete(admin, key).status()).isEqualTo(204);
      assertThat(FILES).doesNotContainKey(key);
      assertThat(delete(admin, key).status()).isEqualTo(204);
   }

   @Test
   void aKeyThatTriesToLeaveTheOwnersFolderIsNotTheirs() throws Exception {
      UUID user = UUID.randomUUID();
      FILES.put("images/other/secret.png", "image/png");

      assertThat(delete(TestTokens.sellerUser(user), "images/" + user + "/../other/secret.png").status()).isEqualTo(403);
      assertThat(FILES).containsKey("images/other/secret.png");
   }
}
