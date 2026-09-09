package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.inventory.Inventory;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockAddedConsumer implements InboxUseCase<Inventory.StockAdded> {

   private final CatalogProjection projection;

   @Override
   public Class<Inventory.StockAdded> subscribe() {
      return Inventory.StockAdded.class;
   }

   @Override
   public Void execute(InboxEvent<Inventory.StockAdded> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
