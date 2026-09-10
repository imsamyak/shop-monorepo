package com.ecom.inventory.service.item;

import com.ecom.contract.inventory.Inventory.StockAdded;
import com.ecom.inventory.domain.entity.InventoryItem;
import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.port.in.usecase.item.AddStockUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.command.AddStockCmd;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryForUpdatePort;
import com.ecom.inventory.port.out.persistence.item.SaveInventoryPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Adds stock to an existing item. Items are created by the {@code Sku.Created} event, never by this call, so stock
 * can only be added to a real SKU.
 */
@Service
@Validated
@RequiredArgsConstructor
public class AddStockService implements AddStockUseCase {

   private final LoadInventoryForUpdatePort loadInventoryForUpdatePort;
   private final SaveInventoryPort saveInventoryPort;
   private final InventoryMapper inventoryMapper;

   @Override
   @Transactional
   public InventoryResult execute(AddStockCmd cmd) {
      InventoryItem item = loadInventoryForUpdatePort.loadBySkuIdForUpdate(cmd.skuId())
            .orElseThrow(() -> InventoryNotFoundException.of(cmd.skuId()));
      OwnershipGuard.requireSellerOr(item.getSellerId(), UserRole.ROLE_ADMIN);

      item.addStock(cmd.quantity());

      return inventoryMapper.toResult(saveInventoryPort.save(item), cmd.quantity());
   }

   @Override
   public DomainEvent buildEvent(AddStockCmd cmd, InventoryResult result) {
      if (result == null) {
         return null;
      }
      return new StockAdded(String.valueOf(result.skuId()), cmd.quantity(), result.totalQuantity(),
            result.availableQuantity(), result.version());
   }
}
