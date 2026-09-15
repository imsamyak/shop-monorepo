import { useCase } from '../../core/use-case-builder.js';
import { listProductsQuery, productParams } from './schemas.js';
import type { CatalogProduct, CatalogProductPage } from './types.js';

export type * from './types.js';

/** The home page listing; newest first unless `sort` says otherwise. */
export const listCatalogProducts = useCase('ListCatalogProducts')
  .service('catalog')
  .get('/catalog/products')
  .query(listProductsQuery)
  .returns<CatalogProductPage>()
  .build();

export const getCatalogProduct = useCase('GetCatalogProduct')
  .service('catalog')
  .get('/catalog/products/{id}')
  .params(productParams)
  .returns<CatalogProduct>()
  .build();

export const catalogUseCases = [listCatalogProducts, getCatalogProduct] as const;
