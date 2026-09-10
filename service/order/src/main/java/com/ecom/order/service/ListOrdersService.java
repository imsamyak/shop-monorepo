package com.ecom.order.service;

import com.ecom.order.port.in.usecase.ListOrdersUseCase;
import com.ecom.order.port.in.usecase.dto.query.ListOrdersQuery;
import com.ecom.order.port.in.usecase.dto.result.OrdersResult;
import com.ecom.order.port.out.persistence.SearchOrdersPort;
import com.ecom.order.service.mapper.OrderMapper;
import com.ecom.security.Caller;
import com.ecom.shared.exception.security.ForbiddenException;
import com.ecom.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ListOrdersService implements ListOrdersUseCase {

   private final SearchOrdersPort searchOrdersPort;
   private final OrderMapper orderMapper;

   @Override
   public OrdersResult execute(ListOrdersQuery query) {
      UUID customerId = visibleTo(query.customerId());
      SearchOrdersPort.Found found = searchOrdersPort.search(customerId, query.status(), query.page(), query.size());
      return new OrdersResult(
            found.orders().stream().map(orderMapper::toResult).toList(),
            query.page(),
            query.size(),
            found.totalElements());
   }

   /**
    * An admin may look at anyone's orders (or everyone's); anybody else only at their own, so asking for another
    * customer's orders is refused instead of quietly answered with the caller's.
    */
   private static UUID visibleTo(UUID requested) {
      if (Caller.hasRole(UserRole.ROLE_ADMIN)) {
         return requested;
      }
      UUID caller = Caller.userId();
      if (requested != null && !requested.equals(caller)) {
         throw ForbiddenException.of("You can only list your own orders");
      }
      return caller;
   }
}
