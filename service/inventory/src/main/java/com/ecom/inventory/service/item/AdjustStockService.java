package com.ecom.inventory.service.item;

import com.ecom.contract.inventory.Inventory.StockAdjusted;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.port.in.usecase.item.AdjustStockUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.command.AdjustStockCmd;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AdjustStockService implements AdjustStockUseCase {

   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final InventoryMapper inventoryMapper;

   @Override
   @Transactional
   public InventoryResult execute(AdjustStockCmd cmd) {
      InventoryItem item = loadInventoryForUpdatePort.loadBySkuIdForUpdate(cmd.skuId())
            .orElseThrow(() -> InventoryNotFoundException.of(cmd.skuId()));

      int previousTotal = item.getTotalQuantity();
      item.adjustTotalQuantity(cmd.totalQuantity());

      return inventoryMapper.toResult(saveInventoryPort.save(item), item.getTotalQuantity() - previousTotal);
   }

   @Override
   public DomainEvent buildEvent(AdjustStockCmd cmd, InventoryResult result) {
      if (result == null) {
         return null;
      }
      return new StockAdjusted(String.valueOf(result.skuId()), result.quantityChange(), result.totalQuantity());
   }
}
