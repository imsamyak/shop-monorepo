package com.ecom.listing.service.product;

import com.ecom.contract.product.Product.Created;
import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.port.in.usecase.product.CreateProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.CreateProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.listing.port.out.persistence.product.SaveProductPort;
import com.ecom.listing.service.product.mapper.ProductMapper;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

   private final SaveProductPort saveProductPort;
   private final ProductMapper productMapper;

   @Override
   @Transactional
   public ProductResult execute(CreateProductCmd cmd) {
      Product product = Product.of(cmd.sellerId(), cmd.title(), cmd.description(), cmd.defaultPrice());
      return productMapper.toResult(saveProductPort.save(product));
   }

   @Override
   public DomainEvent buildEvent(CreateProductCmd cmd, ProductResult data) {
      if (data == null) {
         return null;
      }
      return new Created(
          data.id().toString(),
          data.sellerId().toString(),
          data.title(),
          data.description(),
          data.defaultPrice(),
          data.version()
      );
   }
}
