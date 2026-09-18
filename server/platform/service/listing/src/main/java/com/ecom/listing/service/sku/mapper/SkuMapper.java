package com.ecom.listing.service.sku.mapper;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.port.in.usecase.sku.dto.command.CreateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.command.UpdateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SkuMapper {

   public Sku toEntity(CreateSkuCmd cmd, Product product) {
      BigDecimal price = null;
      if (cmd.price() != null && cmd.price().isPresent()) {
         price = cmd.price().get();
      } else if (product != null) {
         price = product.getDefaultPrice();
      }

      Sku sku = Sku.builder()
            .product(product)
            .price(price)
            .build();

      if (cmd.properties() != null) {
         sku.setProperties(cmd.properties());
      }
      return sku;
   }

   public void updateEntity(Sku sku, UpdateSkuCmd cmd) {
      if (cmd.price() != null) {
         sku.setPrice(cmd.price());
      }
      if (cmd.properties() != null) {
         if (cmd.properties().remove() != null) {
            cmd.properties().remove().forEach(sku::removeProperty);
         }
         if (cmd.properties().add() != null) {
            sku.setProperties(cmd.properties().add());
         }
      }
   }

   public SkuResult toResult(Sku sku) {
      if (sku == null) {
         return null;
      }
      return new SkuResult(
            sku.getId(),
            sku.getProduct() != null ? sku.getProduct().getId() : null,
            sku.getSellerId(),
            sku.getPrice(),
            sku.getProperties() != null ? Collections.unmodifiableMap(sku.getProperties()) : Map.of(),
            sku.version()
      );
   }

   public List<SkuResult> toResultList(Collection<Sku> skus) {
      if (skus == null) {
         return List.of();
      }
      return skus.stream().map(this::toResult).toList();
   }
}
