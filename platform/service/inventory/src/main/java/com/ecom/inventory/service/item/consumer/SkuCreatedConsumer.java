package com.ecom.inventory.service.item.consumer;

import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates the (empty) inventory item of every new SKU, owned by the SKU's seller. Idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuCreatedConsumer implements InboxUseCase<Sku.Created> {

   private final LoadInventoryPort loadInventoryPort;
   private final SaveInventoryPort saveInventoryPort;

   @Override
   public Class<Sku.Created> subscribe() {
      return Sku.Created.class;
   }

   @Override
   @Transactional
   public Void execute(InboxEvent<Sku.Created> cmd) {
      Long skuId = Long.parseLong(cmd.event().skuId());
      UUID sellerId = UUID.fromString(cmd.event().sellerId());

      if (loadInventoryPort.loadBySkuId(skuId).isEmpty()) {
         saveInventoryPort.save(InventoryItem.of(skuId, sellerId, 0));
         log.info("Initialized inventory record for skuId={}", skuId);
      } else {
         log.info("Inventory record already exists for skuId={}", skuId);
      }
      return null;
   }
}
