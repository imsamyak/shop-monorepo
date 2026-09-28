import type { Decimal, IsoInstant, PageResponse, Uuid } from '../shared/types.js';

export type OrderStatus = 'DRAFT' | 'COMMITTED' | 'CANCELLED';

export interface OrderItemResponse {
  id: number;
  skuId: number;
  sellerId: Uuid;
  quantity: number;
  unitPrice: Decimal;
  subtotal: Decimal;
}

export interface OrderResponse {
  /** Same as the checkout id it was built from. */
  orderId: Uuid;
  customerId: Uuid;
  status: OrderStatus;
  totalAmount: Decimal;
  version: number;
  /** When the order was first recorded; null for an order the shop only knows from a late event. */
  createdAt: IsoInstant | null;
  items: OrderItemResponse[];
}

export type OrdersResponse = PageResponse<OrderResponse>;
