import type { Decimal, PageResponse, Uuid } from '../shared/types.js';

export interface ProductResponse {
  id: Uuid;
  sellerId: Uuid;
  title: string;
  description: string | null;
  defaultPrice: Decimal;
  version: number;
  deletedSkuIds: number[];
  /** Picture addresses in display order; the first is the main one. */
  images: string[];
}

export interface ProductPageResponse extends PageResponse<ProductResponse> {
  nextPage: number | null;
}

export interface SkuResponse {
  skuId: number;
  productId: Uuid;
  sellerId: Uuid;
  price: Decimal;
  properties: Record<string, string>;
  version: number;
}
