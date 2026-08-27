package com.ecom.shared.web;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One exception-to-HTTP mapping shared by every service. Extends {@link ResponseEntityExceptionHandler} so framework
 * errors keep their proper status (400 malformed body / bad parameter, 404 unknown route, 405, 415, ...) instead of
 * collapsing into 500. Unexpected errors are logged.
 */
@Slf4j
@RestControllerAdvice
public class SharedExceptionHandler extends ResponseEntityExceptionHandler {

   @ExceptionHandler(BaseException.class)
   public ResponseEntity<Object> handleBase(BaseException ex, WebRequest request) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
      problem.setTitle(ex.getErrorCode().name());
      problem.setProperty("errorCode", ex.getErrorCode().name());
      problem.setProperty("timestamp", ex.getTimestamp());
      if (ex.getDetails() != null && !ex.getDetails().isEmpty()) {
         problem.setProperty("details", ex.getDetails());
      }
      if (ex.getHttpStatus().is5xxServerError()) {
         log.error("Request failed: {}", ex.getMessage(), ex);
      }
      return handleExceptionInternal(ex, problem, new HttpHeaders(), ex.getHttpStatus(), request);
   }

   @ExceptionHandler(OptimisticLockingFailureException.class)
   public ResponseEntity<Object> handleOptimisticLock(OptimisticLockingFailureException ex, WebRequest request) {
      return respond(ex, request, HttpStatus.CONFLICT, ErrorCode.VERSION_CONFLICT,
            "The resource was modified by someone else. Reload it and try again.");
   }

   @ExceptionHandler(PessimisticLockingFailureException.class)
   public ResponseEntity<Object> handlePessimisticLock(PessimisticLockingFailureException ex, WebRequest request) {
      return respond(ex, request, HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION,
            "The resource is busy. Retry shortly.");
   }

   @ExceptionHandler(DataIntegrityViolationException.class)
   public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
      log.warn("Data integrity violation: {}", rootMessage(ex));
      return respond(ex, request, HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE,
            "The request conflicts with existing data.");
   }

   @ExceptionHandler(ConstraintViolationException.class)
   public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
      Map<String, String> invalid = new LinkedHashMap<>();
      for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
         invalid.put(violation.getPropertyPath().toString(), violation.getMessage());
      }
      problem.setProperty("invalidFields", invalid);
      problem.setProperty("errorCode", ErrorCode.VALIDATION_ERROR.name());
      return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
   }

   @Override
   protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
      Map<String, String> invalid = new LinkedHashMap<>();
      ex.getBindingResult().getFieldErrors().forEach(e -> invalid.putIfAbsent(e.getField(), e.getDefaultMessage()));
      ex.getBindingResult().getGlobalErrors().forEach(e -> invalid.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
      problem.setProperty("invalidFields", invalid);
      problem.setProperty("errorCode", ErrorCode.VALIDATION_ERROR.name());
      return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
      log.error("Unhandled exception", ex);
      return respond(ex, request, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
   }

   @Override
   protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
      ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
      if (response != null && response.getBody() instanceof ProblemDetail problem) {
         enrich(problem, statusCode);
      }
      return response;
   }

   private ResponseEntity<Object> respond(Exception ex, WebRequest request, HttpStatus status, ErrorCode code, String detail) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
      problem.setTitle(code.name());
      problem.setProperty("errorCode", code.name());
      return handleExceptionInternal(ex, problem, new HttpHeaders(), status, request);
   }

   private static void enrich(ProblemDetail problem, HttpStatusCode status) {
      Map<String, Object> properties = problem.getProperties();
      if (properties == null || !properties.containsKey("errorCode")) {
         problem.setProperty("errorCode", codeFor(status).name());
      }
      if (properties == null || !properties.containsKey("timestamp")) {
         problem.setProperty("timestamp", Instant.now());
      }
   }

   private static ErrorCode codeFor(HttpStatusCode status) {
      int value = status.value();
      if (value == 404) return ErrorCode.RESOURCE_NOT_FOUND;
      if (value == 401) return ErrorCode.UNAUTHORIZED;
      if (value == 403) return ErrorCode.FORBIDDEN;
      if (value == 409) return ErrorCode.DUPLICATE_RESOURCE;
      if (value >= 500) return ErrorCode.INTERNAL_SERVER_ERROR;
      return ErrorCode.BAD_REQUEST;
   }

   private static String rootMessage(Throwable ex) {
      Throwable root = ex;
      while (root.getCause() != null) {
         root = root.getCause();
      }
      return root.getMessage();
   }
}
