package com.ecom.shared.exception.dependency;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * A service this one depends on could not be reached or failed unexpectedly. Maps to 503 so callers know the
 * request may be retried.
 */
public class DependencyUnavailableException extends BaseException {

   public DependencyUnavailableException(String dependency, String message, Throwable cause) {
      super(dependency + " is unavailable: " + message, ErrorCode.SERVICE_UNAVAILABLE,
          HttpStatus.SERVICE_UNAVAILABLE, cause, Map.of("dependency", dependency));
   }

   public static DependencyUnavailableException of(String dependency, String message, Throwable cause) {
      return new DependencyUnavailableException(dependency, message, cause);
   }
}
