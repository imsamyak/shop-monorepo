package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkuCreatedConsumer implements InboxUseCase<Sku.Created> {

   private final CatalogProjection projection;

   @Override
   public Class<Sku.Created> subscribe() {
      return Sku.Created.class;
   }

   @Override
   public Void execute(InboxEvent<Sku.Created> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
