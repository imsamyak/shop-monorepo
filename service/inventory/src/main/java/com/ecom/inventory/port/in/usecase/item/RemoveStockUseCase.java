package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.command.RemoveStockCmd;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.outbox.OutboxUseCase;

public interface RemoveStockUseCase extends OutboxUseCase<RemoveStockCmd, InventoryResult> {
}
