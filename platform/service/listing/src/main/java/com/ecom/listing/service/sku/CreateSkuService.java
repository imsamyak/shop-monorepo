package com.ecom.listing.service.sku;

import com.ecom.contract.product.Sku.Created;
import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.domain.exception.sku.DuplicateSkuPropertiesException;
import com.ecom.listing.port.in.usecase.sku.CreateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.CreateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.port.out.persistence.sku.SaveSkuPort;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CreateSkuService implements CreateSkuUseCase {

   private final LoadProductPort loadProductPort;
   private final LoadSkuPort loadSkuPort;
   private final SaveSkuPort saveSkuPort;
   private final SkuMapper skuMapper;

   @Override
   @Transactional
   public SkuResult execute(CreateSkuCmd cmd) {
      Product product = loadProductPort.load(cmd.productId())
          .orElseThrow(() -> ProductNotFoundException.of(cmd.productId()));
      OwnershipGuard.requireSellerOr(product.getSellerId(), UserRole.ROLE_ADMIN);


      Sku sku = skuMapper.toEntity(cmd, product);
      if (loadSkuPort.existsWithProperties(product.getId(), sku.getProperties(), null)) {
         throw DuplicateSkuPropertiesException.of(product.getId(), sku.getProperties());
      }

      return skuMapper.toResult(saveSkuPort.save(sku));
   }

   @Override
   public DomainEvent buildEvent(CreateSkuCmd cmd, SkuResult data) {
      if (data == null) {
         return null;
      }
      return new Created(
          data.skuId().toString(),
          data.productId().toString(),
          data.sellerId().toString(),
          data.price(),
          data.properties(),
          data.version()
      );
   }
}
