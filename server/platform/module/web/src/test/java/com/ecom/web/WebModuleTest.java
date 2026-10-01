package com.ecom.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

// the controller is named here because Spring Boot does not scan the nested classes of a test
@SpringBootTest(classes = {WebModuleTest.App.class, WebModuleTest.PingController.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebModuleTest {

   @SpringBootApplication
   static class App {
   }

   @RestController
   public static class PingController {
      @GetMapping("/ping")
      public String ping() {
         return "pong";
      }
   }

   @Value("${local.server.port}")
   private int port;

   private final HttpClient http = HttpClient.newHttpClient();

   private HttpResponse<String> send(String method, String path) throws Exception {
      HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method(method, HttpRequest.BodyPublishers.noBody()).build();
      return http.send(request, HttpResponse.BodyHandlers.ofString());
   }

   @Test
   void answersHealthWithoutAnythingElse() throws Exception {
      HttpResponse<String> response = send("GET", "/health");
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("{\"status\":\"ok\"}");
      assertThat(send("HEAD", "/health").statusCode()).isEqualTo(200);
   }

   @Test
   void healthIsNotUnderTheApiPrefix() throws Exception {
      assertThat(send("GET", "/api/health").statusCode()).isEqualTo(404);
   }

   @Test
   void healthOnlyAnswersGetAndHead() throws Exception {
      assertThat(send("POST", "/health").statusCode()).isNotEqualTo(200);
   }

   @Test
   void putsApiInFrontOfControllers() throws Exception {
      HttpResponse<String> prefixed = send("GET", "/api/ping");
      assertThat(prefixed.statusCode()).isEqualTo(200);
      assertThat(prefixed.body()).isEqualTo("pong");
      assertThat(send("GET", "/ping").statusCode()).isEqualTo(404);
   }
}
