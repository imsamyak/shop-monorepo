package com.ecom.listing.port.in.usecase.product;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.UpdateProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;

public interface UpdateProductUseCase
   extends OutboxUseCase<UpdateProductCmd, ProductResult> {

}
