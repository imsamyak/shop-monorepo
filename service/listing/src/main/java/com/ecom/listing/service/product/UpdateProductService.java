package com.ecom.listing.service.product;

import com.ecom.contract.product.Product.Updated;
import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.port.in.usecase.product.UpdateProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.UpdateProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.listing.port.out.persistence.product.UpdateProductPort;
import com.ecom.listing.service.product.mapper.ProductMapper;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.validator.EntityVersionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UpdateProductService implements UpdateProductUseCase {

   private final UpdateProductPort updateProductPort;
   private final ProductMapper productMapper;
   private final EntityVersionValidator versionValidator;

   @Override
   @Transactional
   public ProductResult execute(UpdateProductCmd cmd) {
      Product product = updateProductPort.loadForUpdate(cmd.id())
          .orElseThrow(() -> ProductNotFoundException.of(cmd.id()));

      versionValidator.validate(product, cmd.version());

      productMapper.updateEntity(product, cmd);
      return productMapper.toResult(updateProductPort.save(product));
   }

   @Override
   public DomainEvent buildEvent(UpdateProductCmd cmd, ProductResult data) {
      if (data == null) {
         return null;
      }
      return new Updated(
          data.id().toString(),
          data.title(),
          data.description(),
          data.version()
      );
   }
}
