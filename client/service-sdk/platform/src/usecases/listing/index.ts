import { useCase } from '@shop/sdk-core';
import {
  createProductBody,
  createSkuBody,
  listProductsQuery,
  productParams,
  skuParams,
  skusParams,
  updateProductBody,
  updateSkuBody,
  versionQuery,
} from './schemas.js';
import type { ProductPageResponse, ProductResponse, SkuResponse } from './types.js';

export type * from './types.js';

/** Sellers only: the product belongs to the seller in the access token. */
export const createProduct = useCase('CreateProduct')
  .service('listing')
  .post('/api/products')
  .body(createProductBody)
  .returns<ProductResponse>()
  .authRequired()
  .build();

export const listProducts = useCase('ListProducts')
  .service('listing')
  .get('/api/products')
  .query(listProductsQuery)
  .returns<ProductPageResponse>()
  .build();

export const getProduct = useCase('GetProduct')
  .service('listing')
  .get('/api/products/{id}')
  .params(productParams)
  .returns<ProductResponse>()
  .build();

export const updateProduct = useCase('UpdateProduct')
  .service('listing')
  .patch('/api/products/{id}')
  .params(productParams)
  .body(updateProductBody)
  .returns<ProductResponse>()
  .authRequired()
  .build();

/** The backend handles PUT exactly like PATCH. */
export const replaceProduct = useCase('ReplaceProduct')
  .service('listing')
  .put('/api/products/{id}')
  .params(productParams)
  .body(updateProductBody)
  .returns<ProductResponse>()
  .authRequired()
  .build();

export const deleteProduct = useCase('DeleteProduct')
  .service('listing')
  .delete('/api/products/{id}')
  .params(productParams)
  .query(versionQuery)
  .returns<ProductResponse>()
  .authRequired()
  .build();

export const listSkus = useCase('ListSkus')
  .service('listing')
  .get('/api/products/{productId}/skus')
  .params(skusParams)
  .returns<SkuResponse[]>()
  .build();

export const getSku = useCase('GetSku')
  .service('listing')
  .get('/api/products/{productId}/skus/{skuId}')
  .params(skuParams)
  .returns<SkuResponse>()
  .build();

export const createSku = useCase('CreateSku')
  .service('listing')
  .post('/api/products/{productId}/skus')
  .params(skusParams)
  .body(createSkuBody)
  .returns<SkuResponse>()
  .authRequired()
  .build();

export const updateSku = useCase('UpdateSku')
  .service('listing')
  .put('/api/products/{productId}/skus/{skuId}')
  .params(skuParams)
  .body(updateSkuBody)
  .returns<SkuResponse>()
  .authRequired()
  .build();

export const deleteSku = useCase('DeleteSku')
  .service('listing')
  .delete('/api/products/{productId}/skus/{skuId}')
  .params(skuParams)
  .query(versionQuery)
  .returns<SkuResponse>()
  .authRequired()
  .build();

export const listingUseCases = [
  createProduct,
  listProducts,
  getProduct,
  updateProduct,
  replaceProduct,
  deleteProduct,
  listSkus,
  getSku,
  createSku,
  updateSku,
  deleteSku,
] as const;
