package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.query.ListInventoryQuery;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryPageResult;
import com.ecom.shared.usecase.QueryUseCase;

public interface ListInventoryUseCase extends QueryUseCase<ListInventoryQuery, InventoryPageResult> {
}
