package com.ecom.order.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

import java.util.List;

/**
 * One page of orders, newest first.
 *
 * @param orders        the orders on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements orders matching the filter over all pages
 */
public record OrdersResult(
      List<OrderResult> orders,
      int page,
      int size,
      long totalElements
) implements Result.Many<OrderResult> {

   public OrdersResult {
      orders = orders == null ? List.of() : List.copyOf(orders);
   }

   @Override
   public List<OrderResult> items() {
      return orders;
   }

   public int totalPages() {
      return (int) ((totalElements + size - 1) / size);
   }
}
