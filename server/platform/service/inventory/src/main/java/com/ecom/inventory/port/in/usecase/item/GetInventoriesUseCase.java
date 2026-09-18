package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.query.GetInventoriesQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoriesResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface GetInventoriesUseCase extends QueryUseCase<GetInventoriesQuery, InventoriesResult> {
}
