package com.ecom.shared.exception.security;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, Map<String, Object> details) {
        super(message, ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, details);
    }

    public static UnauthorizedException of(String message) {
        return new UnauthorizedException(message);
    }

    @Builder
    public static UnauthorizedException of(String message, Map<String, Object> details) {
        return new UnauthorizedException(message, details);
    }
}
