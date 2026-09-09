package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkuDeletedConsumer implements InboxUseCase<Sku.Deleted> {

   private final CatalogProjection projection;

   @Override
   public Class<Sku.Deleted> subscribe() {
      return Sku.Deleted.class;
   }

   @Override
   public Void execute(InboxEvent<Sku.Deleted> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
