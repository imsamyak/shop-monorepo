package com.ecom.shared.exception.validation;

import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;

import java.util.Map;

public class InvalidCommandException extends ValidationException {

    public InvalidCommandException(String message) {
        super(message, ErrorCode.INVALID_COMMAND);
    }

    public InvalidCommandException(String message, Map<String, Object> details) {
        super(message, ErrorCode.INVALID_COMMAND, details);
    }

    public static InvalidCommandException of(String message) {
        return new InvalidCommandException(message);
    }

    @Builder
    public static InvalidCommandException of(String message, Map<String, Object> details) {
        return new InvalidCommandException(message, details);
    }
}
