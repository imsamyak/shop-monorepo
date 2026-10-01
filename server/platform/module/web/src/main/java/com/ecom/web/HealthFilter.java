package com.ecom.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Answers {@code GET /health} (and HEAD) with 200 {@code {"status":"ok"}} before anything else looks at the request, so a
 * load balancer or an orchestrator can probe it without a token and without the application's own filters.
 */
public class HealthFilter extends OncePerRequestFilter {

   private static final byte[] BODY = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

   private final String path;

   public HealthFilter(String path) {
      this.path = path;
   }

   @Override
   protected boolean shouldNotFilter(HttpServletRequest request) {
      String method = request.getMethod();
      return !(("GET".equals(method) || "HEAD".equals(method)) && path.equals(request.getRequestURI()));
   }

   @Override
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
       throws ServletException, IOException {
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json");
      response.setHeader("Cache-Control", "no-store");
      response.setContentLength(BODY.length);
      if (!"HEAD".equals(request.getMethod())) {
         response.getOutputStream().write(BODY);
      }
   }
}
