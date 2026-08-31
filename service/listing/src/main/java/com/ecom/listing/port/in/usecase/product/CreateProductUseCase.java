package com.ecom.listing.port.in.usecase.product;

import com.ecom.outbox.OutboxUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.CreateProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;

public interface CreateProductUseCase
   extends OutboxUseCase<CreateProductCmd, ProductResult> {

}
