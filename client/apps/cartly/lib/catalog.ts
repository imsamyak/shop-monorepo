import 'server-only';
import type { CatalogProduct, CatalogSku } from '@shop/platform-sdk';
import { cache } from 'react';
import { api } from './sdk';

export interface SkuInfo {
  product: CatalogProduct;
  sku: CatalogSku;
}

/**
 * Carts and orders only carry SKU ids, so names and images come from the catalog. Reads the newest 100 products,
 * which is plenty for a small shop; unknown SKUs are shown by id.
 */
export const getSkuIndex = cache(async (): Promise<Map<number, SkuInfo>> => {
  const page = await api().catalog.listCatalogProducts({ size: 100 });
  const index = new Map<number, SkuInfo>();
  for (const product of page.items) {
    for (const sku of product.skus) index.set(Number(sku.skuId), { product, sku });
  }
  return index;
});

export function skuLabel(sku: CatalogSku): string {
  const values = Object.values(sku.properties);
  return values.length ? values.join(' / ') : 'Standard';
}
