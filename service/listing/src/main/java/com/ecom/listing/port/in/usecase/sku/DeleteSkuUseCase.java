package com.ecom.listing.port.in.usecase.sku;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.DeleteSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;

public interface DeleteSkuUseCase
   extends OutboxUseCase<DeleteSkuCmd, SkuResult> {

}
