package com.ecom.shared.exception.notfound;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message, ErrorCode errorCode) {
        super(message, errorCode, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message, ErrorCode errorCode, Map<String, Object> details) {
        super(message, errorCode, HttpStatus.NOT_FOUND, details);
    }
}
