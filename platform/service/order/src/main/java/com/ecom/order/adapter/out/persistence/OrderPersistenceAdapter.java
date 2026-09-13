package com.ecom.order.adapter.out.persistence;

import com.ecom.order.domain.entity.Order;
import com.ecom.order.domain.entity.OrderStatus;
import com.ecom.order.port.out.persistence.LoadOrderForUpdatePort;
import com.ecom.order.port.out.persistence.LoadOrderPort;
import com.ecom.order.port.out.persistence.SaveOrderPort;
import com.ecom.order.port.out.persistence.SearchOrdersPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements
      LoadOrderPort,
      LoadOrderForUpdatePort,
      SaveOrderPort,
      SearchOrdersPort {

   /** Newest first; the id keeps the order stable between orders created in the same instant. */
   private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("ts.createdAt"), Sort.Order.desc("id"));

   private final OrderJpaRepository repository;

   @Override
   @Transactional(readOnly = true)
   public Optional<Order> loadById(UUID orderId) {
      return repository.findById(orderId);
   }

   @Override
   @Transactional
   public Optional<Order> loadByIdForUpdate(UUID orderId) {
      return repository.findByIdForUpdate(orderId);
   }

   @Override
   @Transactional
   public Order save(Order order) {
      return repository.saveAndFlush(order);
   }

   @Override
   @Transactional(readOnly = true)
   public Found search(UUID customerId, OrderStatus status, int page, int size) {
      Specification<Order> filter = Specification.unrestricted();
      if (customerId != null) {
         filter = filter.and((root, query, cb) -> cb.equal(root.get("customerId"), customerId));
      }
      if (status != null) {
         filter = filter.and((root, query, cb) -> cb.equal(root.get("status"), status));
      }
      Page<Order> found = repository.findAll(filter, PageRequest.of(page, size, NEWEST_FIRST));
      return new Found(found.getContent(), found.getTotalElements());
   }
}
