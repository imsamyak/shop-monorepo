package com.ecom.shared.exception.domain;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class BusinessRuleException extends BaseException {

    public BusinessRuleException(String message) {
        super(message, ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public BusinessRuleException(String message, Map<String, Object> details) {
        super(message, ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.UNPROCESSABLE_CONTENT, details);
    }

    public BusinessRuleException(String message, ErrorCode errorCode, Map<String, Object> details) {
        super(message, errorCode, HttpStatus.UNPROCESSABLE_CONTENT, details);
    }

    public static BusinessRuleException of(String message) {
        return new BusinessRuleException(message);
    }

    @Builder
    public static BusinessRuleException of(String message, Map<String, Object> details) {
        return new BusinessRuleException(message, details);
    }
}
