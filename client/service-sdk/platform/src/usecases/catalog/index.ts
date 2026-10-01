import { useCase } from '@shop/sdk-core';
import { listProductsQuery, productParams } from './schemas.js';
import type { CatalogProduct, CatalogProductPage } from './types.js';

export type * from './types.js';

/** The home page listing; newest first unless `sort` says otherwise. */
export const listCatalogProducts = useCase('ListCatalogProducts')
  .service('catalog')
  .get('/api/catalog/products')
  .query(listProductsQuery)
  .returns<CatalogProductPage>()
  .build();

export const getCatalogProduct = useCase('GetCatalogProduct')
  .service('catalog')
  .get('/api/catalog/products/{id}')
  .params(productParams)
  .returns<CatalogProduct>()
  .build();

export const catalogUseCases = [listCatalogProducts, getCatalogProduct] as const;
