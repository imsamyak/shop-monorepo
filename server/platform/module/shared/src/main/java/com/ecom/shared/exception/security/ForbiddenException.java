package com.ecom.shared.exception.security;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String message, Map<String, Object> details) {
        super(message, ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, details);
    }

    public static ForbiddenException of(String message) {
        return new ForbiddenException(message);
    }

    @Builder
    public static ForbiddenException of(String message, Map<String, Object> details) {
        return new ForbiddenException(message, details);
    }
}
