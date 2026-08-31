package com.ecom.listing.port.in.usecase.sku;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.UpdateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;

public interface UpdateSkuUseCase
   extends OutboxUseCase<UpdateSkuCmd, SkuResult> {

}
