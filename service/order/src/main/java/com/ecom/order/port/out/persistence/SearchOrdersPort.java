package com.ecom.order.port.out.persistence;

import com.ecom.order.domain.entity.Order;
import com.ecom.order.domain.entity.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface SearchOrdersPort {

   /** One page of the orders matching the filters (null means no filter), newest first. */
   Found search(UUID customerId, OrderStatus status, int page, int size);

   /**
    * @param orders        the orders on the requested page
    * @param totalElements the orders matching the filters over all pages
    */
   record Found(List<Order> orders, long totalElements) {
   }
}
