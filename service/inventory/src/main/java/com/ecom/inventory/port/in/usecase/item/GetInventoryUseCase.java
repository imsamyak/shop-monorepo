package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetInventoryUseCase extends QueryUseCase<GetInventoryQuery, InventoryResult> {
}
