package com.ecom.order.domain.entity;

public enum OrderStatus {
   DRAFT,
   COMMITTED,
   CANCELLED;

   /** A committed or cancelled order never changes again. */
   public boolean isTerminal() {
      return this != DRAFT;
   }
}
