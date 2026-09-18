package com.ecom.inventory.domain.exception.reservation;

import com.ecom.shared.exception.domain.BusinessRuleException;
import lombok.Getter;

import java.util.Map;

@Getter
public class InvalidReservationQuantityException extends BusinessRuleException {

   private final int quantity;

   public InvalidReservationQuantityException(String message, int quantity) {
      super(message, Map.of("quantity", quantity));
      this.quantity = quantity;
   }

   public static InvalidReservationQuantityException of(String message, int quantity) {
      return new InvalidReservationQuantityException(message, quantity);
   }
}
