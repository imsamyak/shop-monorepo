package com.ecom.listing.port.in.usecase.product;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.DeleteProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;

public interface DeleteProductUseCase
   extends OutboxUseCase<DeleteProductCmd, ProductResult> {

}
