package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Product;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductUpdatedConsumer implements InboxUseCase<Product.Updated> {

   private final CatalogProjection projection;

   @Override
   public Class<Product.Updated> subscribe() {
      return Product.Updated.class;
   }

   @Override
   public Void execute(InboxEvent<Product.Updated> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
