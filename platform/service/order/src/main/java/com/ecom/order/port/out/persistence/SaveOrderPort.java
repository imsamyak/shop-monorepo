package com.ecom.order.port.out.persistence;

import com.ecom.order.domain.entity.Order;

public interface SaveOrderPort {

   Order save(Order order);
}
