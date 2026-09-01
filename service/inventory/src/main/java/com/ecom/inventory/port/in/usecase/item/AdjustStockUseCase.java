package com.ecom.inventory.port.in.usecase.item;

import com.ecom.inventory.port.in.usecase.item.dto.command.AdjustStockCmd;
import com.ecom.inventory.port.in.usecase.item.dto.result.InventoryResult;
import com.ecom.outbox.OutboxUseCase;

public interface AdjustStockUseCase extends OutboxUseCase<AdjustStockCmd, InventoryResult> {
}
