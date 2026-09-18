package com.ecom.inventory.service.item.consumer;

import com.ecom.contract.product.Sku;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.entity.InventoryReservation;
import com.ecom.inventory.port.out.persistence.item.DeleteInventoryPort;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationPort;
import com.ecom.inventory.port.out.persistence.reservation.LoadReservationForUpdatePort;
import com.ecom.inventory.port.out.persistence.reservation.SaveReservationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Removes the inventory of a deleted SKU. Pending reservations of that SKU are released first (the item can no longer
 * be bought), so deleting never strands a reservation that points at nothing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkuDeletedConsumer implements InboxUseCase<Sku.Deleted> {

   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final LoadReservationPort loadReservationPort;
   private final LoadReservationForUpdatePort loadReservationForUpdatePort;
   private final SaveReservationPort saveReservationPort;
   private final SaveInventoryPort saveInventoryPort;
   private final DeleteInventoryPort deleteInventoryPort;

   @Override
   public Class<Sku.Deleted> subscribe() {
      return Sku.Deleted.class;
   }

   @Override
   @Transactional
   public Void execute(InboxEvent<Sku.Deleted> cmd) {
      Long skuId = Long.parseLong(cmd.event().skuId());

      Optional<InventoryItem> item = loadInventoryForUpdatePort.loadBySkuIdForUpdate(skuId);
      if (item.isEmpty()) {
         log.info("No inventory record for deleted skuId={}", skuId);
         return null;
      }

      for (InventoryReservation pending : loadReservationPort.findPendingBySkuId(skuId)) {
         loadReservationForUpdatePort.loadByIdForUpdate(pending.getId())
               .filter(InventoryReservation::isPending)
               .ifPresent(reservation -> {
                  item.get().release(reservation.getQuantity());
                  reservation.release();
                  saveReservationPort.save(reservation);
               });
      }
      saveInventoryPort.save(item.get());

      deleteInventoryPort.deleteBySkuId(skuId);
      log.info("Deleted inventory record for skuId={}", skuId);
      return null;
   }
}
