package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SkuUpdatedConsumer implements InboxUseCase<Sku.Updated> {

   private final CatalogProjection projection;

   @Override
   public Class<Sku.Updated> subscribe() {
      return Sku.Updated.class;
   }

   @Override
   public Void execute(InboxEvent<Sku.Updated> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
