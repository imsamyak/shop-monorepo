package com.ecom.catalog.service;

import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.port.in.usecase.dto.ProductResult;

/**
 * Turns the stored product into what the API shows: stock quantities stop here.
 */
final class CatalogMapper {

   private CatalogMapper() {
   }

   static ProductResult toResult(CatalogProduct product) {
      return new ProductResult(
            product.id(),
            product.sellerId(),
            product.title(),
            product.description(),
            product.price(),
            product.lowestSkuPrice(),
            product.availability(),
            product.createdAt(),
            product.skus().stream()
                  .map(sku -> new ProductResult.SkuResult(sku.skuId(), sku.price(), sku.properties(), sku.availability()))
                  .toList());
   }
}
