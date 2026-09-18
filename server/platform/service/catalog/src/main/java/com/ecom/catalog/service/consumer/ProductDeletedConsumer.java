package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Product;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductDeletedConsumer implements InboxUseCase<Product.Deleted> {

   private final CatalogProjection projection;

   @Override
   public Class<Product.Deleted> subscribe() {
      return Product.Deleted.class;
   }

   @Override
   public Void execute(InboxEvent<Product.Deleted> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
