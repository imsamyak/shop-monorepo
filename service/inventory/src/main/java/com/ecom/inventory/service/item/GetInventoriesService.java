package com.ecom.inventory.service.item;

import com.ecom.inventory.port.in.usecase.item.GetInventoriesUseCase;
import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoriesQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoriesResult;
import com.ecom.inventory.port.out.persistence.item.LoadInventoryPort;
import com.ecom.inventory.service.item.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class GetInventoriesService implements GetInventoriesUseCase {

   private final LoadInventoryPort loadInventoryPort;
   private final InventoryMapper inventoryMapper;

   @Override
   public InventoriesResult execute(GetInventoriesQuery query) {
      return new InventoriesResult(loadInventoryPort.loadBySkuIds(Set.copyOf(query.skuIds())).stream()
            .map(item -> inventoryMapper.toResult(item, 0))
            .toList());
   }
}
