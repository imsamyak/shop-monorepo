import type { Decimal, IsoInstant, PageResponse } from '../shared/types.js';

export type Availability = 'AVAILABLE' | 'OUT_OF_STOCK';

export interface CatalogSku {
  skuId: string;
  price: Decimal;
  properties: Record<string, string>;
  availability: Availability;
}

export interface CatalogProduct {
  id: string;
  sellerId: string;
  title: string;
  description: string;
  /** Picture addresses in display order; the first is the main one. */
  images: string[];
  price: Decimal;
  lowestSkuPrice: Decimal | null;
  availability: Availability;
  createdAt: IsoInstant;
  skus: CatalogSku[];
}

export type CatalogProductPage = PageResponse<CatalogProduct>;
