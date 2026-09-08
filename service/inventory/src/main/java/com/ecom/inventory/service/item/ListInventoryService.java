package com.ecom.inventory.service.item;

import com.ecom.inventory.port.in.usecase.item.ListInventoryUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.query.ListInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryPageResult;
import com.ecom.inventory.port.out.persistence.item.InventoryPage;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ListInventoryService implements ListInventoryUseCase {

   private final LoadInventoryPort loadInventoryPort;
   private final InventoryMapper inventoryMapper;

   @Override
   public InventoryPageResult execute(ListInventoryQuery query) {
      InventoryPage page = loadInventoryPort.loadPage(query.page(), query.size());
      return new InventoryPageResult(
            page.items().stream().map(item -> inventoryMapper.toResult(item, 0)).toList(),
            query.page(),
            query.size(),
            page.totalElements(),
            page.totalPages());
   }
}
