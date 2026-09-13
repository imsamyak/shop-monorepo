package com.ecom.order.service;

import com.ecom.order.domain.exception.OrderNotFoundException;
import com.ecom.order.port.in.usecase.GetOrderUseCase;
import com.ecom.order.port.in.usecase.dto.query.GetOrderQuery;
import com.ecom.order.port.in.usecase.dto.result.OrderResult;
import com.ecom.order.port.out.persistence.LoadOrderPort;
import com.ecom.order.service.mapper.OrderMapper;
import com.ecom.order.domain.entity.Order;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetOrderService implements GetOrderUseCase {

   private final LoadOrderPort loadOrderPort;
   private final OrderMapper orderMapper;

   @Override
   public OrderResult execute(GetOrderQuery query) {
      Order order = loadOrderPort.loadById(query.orderId())
            .orElseThrow(() -> OrderNotFoundException.of(query.orderId()));
      OwnershipGuard.requireUserOr(order.getCustomerId(), UserRole.ROLE_ADMIN);
      return orderMapper.toResult(order);
   }
}
