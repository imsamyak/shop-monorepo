package com.ecom.shared.exception.conflict;

import com.ecom.shared.exception.BaseException;
import com.ecom.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ConflictException extends BaseException {

   public ConflictException(String message, ErrorCode errorCode) {
      super(message, errorCode, HttpStatus.CONFLICT);
   }

   public ConflictException(String message, ErrorCode errorCode, Map<String, Object> details) {
      super(message, errorCode, HttpStatus.CONFLICT, details);
   }

}
