package com.ecom.order.domain.exception;

import com.ecom.shared.exception.notfound.EntityNotFoundException;
import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderNotFoundException extends EntityNotFoundException {

   private final UUID orderId;

   public OrderNotFoundException(UUID orderId) {
      super("Order", orderId);
      this.orderId = orderId;
   }

   public static OrderNotFoundException of(UUID orderId) {
      return new OrderNotFoundException(orderId);
   }
}
