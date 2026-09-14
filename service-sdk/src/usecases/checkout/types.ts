import type { Decimal, IsoInstant, Uuid } from '../shared/types.js';

export interface CartItemResponse {
  skuId: number;
  quantity: number;
}

export interface CartResponse {
  customerId: Uuid;
  items: CartItemResponse[];
  version: number;
  updatedAt: IsoInstant;
}

export type CheckoutStatus = 'DRAFT' | 'COMMITTED' | 'CANCELLED' | 'EXPIRED';

export interface CheckoutItemResponse {
  id: number;
  skuId: number;
  sellerId: Uuid;
  quantity: number;
  unitPrice: Decimal;
  subtotal: Decimal;
}

export interface CheckoutResponse {
  checkoutId: Uuid;
  customerId: Uuid;
  status: CheckoutStatus;
  totalAmount: Decimal;
  expiresAt: IsoInstant;
  version: number;
  items: CheckoutItemResponse[];
}
