package com.ecom.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final Instant timestamp;
    private final Map<String, Object> details;

    protected BaseException(String message, ErrorCode errorCode, HttpStatus httpStatus) {
        this(message, errorCode, httpStatus, null, Collections.emptyMap());
    }

    protected BaseException(String message, ErrorCode errorCode, HttpStatus httpStatus, Throwable cause) {
        this(message, errorCode, httpStatus, cause, Collections.emptyMap());
    }

    protected BaseException(String message, ErrorCode errorCode, HttpStatus httpStatus, Map<String, Object> details) {
        this(message, errorCode, httpStatus, null, details);
    }

    protected BaseException(String message, ErrorCode errorCode, HttpStatus httpStatus, Throwable cause, Map<String, Object> details) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
        this.details = details != null ? Collections.unmodifiableMap(details) : Collections.emptyMap();
    }
}
