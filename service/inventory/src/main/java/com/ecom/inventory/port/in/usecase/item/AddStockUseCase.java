package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.command.AddStockCmd;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.outbox.OutboxUseCase;

public interface AddStockUseCase extends OutboxUseCase<AddStockCmd, InventoryResult> {
}
