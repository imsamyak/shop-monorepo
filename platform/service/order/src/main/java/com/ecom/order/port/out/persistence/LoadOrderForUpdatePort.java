package com.ecom.order.port.out.persistence;

import com.ecom.order.domain.entity.Order;

import java.util.Optional;
import java.util.UUID;

public interface LoadOrderForUpdatePort {

   Optional<Order> loadByIdForUpdate(UUID orderId);
}
