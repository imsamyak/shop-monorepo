package com.ecom.order.service;

import com.ecom.order.port.in.usecase.ListOrdersUseCase;
import com.ecom.order.port.in.usecase.dto.query.ListOrdersQuery;
import com.ecom.order.port.in.usecase.dto.result.OrdersResult;
import com.ecom.order.port.out.persistence.SearchOrdersPort;
import com.ecom.order.service.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ListOrdersService implements ListOrdersUseCase {

   private final SearchOrdersPort searchOrdersPort;
   private final OrderMapper orderMapper;

   @Override
   public OrdersResult execute(ListOrdersQuery query) {
      SearchOrdersPort.Found found = searchOrdersPort.search(query.customerId(), query.status(), query.page(), query.size());
      return new OrdersResult(
            found.orders().stream().map(orderMapper::toResult).toList(),
            query.page(),
            query.size(),
            found.totalElements());
   }
}
