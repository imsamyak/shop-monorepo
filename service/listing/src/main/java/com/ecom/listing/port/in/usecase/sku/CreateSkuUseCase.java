package com.ecom.listing.port.in.usecase.sku;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.CreateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;

public interface CreateSkuUseCase
   extends OutboxUseCase<CreateSkuCmd, SkuResult> {

}
