package com.ecom.listing.service.product;

import com.ecom.contract.product.Product.Deleted;
import com.ecom.contract.product.Sku;
import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.port.in.usecase.product.DeleteProductUseCase;
import com.ecom.listing.port.in.usecase.product.dto.command.DeleteProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.listing.port.out.persistence.product.DeleteProductPort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.port.out.persistence.sku.SaveSkuPort;
import com.ecom.listing.service.product.mapper.ProductMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.validator.EntityVersionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft-deletes a product together with all its SKUs. One {@code Sku.Deleted} event is emitted per SKU, followed by
 * {@code Product.Deleted}, so inventory and the read models stay in sync.
 */
@Service
@Validated
@RequiredArgsConstructor
public class DeleteProductService implements DeleteProductUseCase {

   private final DeleteProductPort deleteProductPort;
   private final LoadSkuPort loadSkuPort;
   private final SaveSkuPort saveSkuPort;
   private final ProductMapper productMapper;
   private final EntityVersionValidator versionValidator;

   @Override
   @Transactional
   public ProductResult execute(DeleteProductCmd cmd) {
      Product product = deleteProductPort.loadForUpdate(cmd.id())
          .orElseThrow(() -> ProductNotFoundException.of(cmd.id()));

      OwnershipGuard.requireSellerOr(product.getSellerId(), UserRole.ROLE_ADMIN);
      versionValidator.validate(product, cmd.version());

      List<Long> deletedSkuIds = new ArrayList<>();
      for (com.ecom.listing.domain.entity.Sku sku : loadSkuPort.loadByProduct(product.getId())) {
         if (sku.softDelete()) {
            saveSkuPort.save(sku);
            deletedSkuIds.add(sku.getId());
         }
      }

      product.markAsDeleted();
      Product saved = deleteProductPort.save(product);

      return productMapper.toResult(saved).withDeletedSkuIds(deletedSkuIds);
   }

   @Override
   public List<DomainEvent> buildEvents(DeleteProductCmd cmd, ProductResult data) {
      if (data == null) {
         return List.of();
      }
      String productId = data.id().toString();
      List<DomainEvent> events = new ArrayList<>();
      for (Long skuId : data.deletedSkuIds()) {
         events.add(new Sku.Deleted(skuId.toString(), productId, data.version()));
      }
      events.add(new Deleted(productId, data.version()));
      return events;
   }
}
