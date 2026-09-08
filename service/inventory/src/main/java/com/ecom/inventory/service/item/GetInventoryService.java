package com.ecom.inventory.service.item;

import com.ecom.inventory.domain.exception.item.InventoryNotFoundException;
import com.ecom.inventory.port.in.usecase.item.GetInventoryUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetInventoryService implements GetInventoryUseCase {

   private final LoadInventoryPort loadInventoryPort;
   private final InventoryMapper inventoryMapper;

   @Override
   public InventoryResult execute(GetInventoryQuery query) {
      return loadInventoryPort.loadBySkuId(query.skuId())
            .map(item -> inventoryMapper.toResult(item, 0))
            .orElseThrow(() -> InventoryNotFoundException.of(query.skuId()));
   }
}
