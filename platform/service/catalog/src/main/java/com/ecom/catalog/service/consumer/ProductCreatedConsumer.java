package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Product;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductCreatedConsumer implements InboxUseCase<Product.Created> {

   private final CatalogProjection projection;

   @Override
   public Class<Product.Created> subscribe() {
      return Product.Created.class;
   }

   @Override
   public Void execute(InboxEvent<Product.Created> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
