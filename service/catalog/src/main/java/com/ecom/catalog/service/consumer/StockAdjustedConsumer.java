package com.ecom.catalog.service.consumer;

import com.ecom.catalog.service.CatalogProjection;
import com.ecom.contract.inventory.Inventory;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockAdjustedConsumer implements InboxUseCase<Inventory.StockAdjusted> {

   private final CatalogProjection projection;

   @Override
   public Class<Inventory.StockAdjusted> subscribe() {
      return Inventory.StockAdjusted.class;
   }

   @Override
   public Void execute(InboxEvent<Inventory.StockAdjusted> cmd) {
      projection.apply(cmd.event());
      return null;
   }
}
