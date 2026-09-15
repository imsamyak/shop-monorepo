import type { IsoInstant, PageResponse, Uuid } from '../shared/types.js';

export interface InventoryResponse {
  skuId: number;
  totalQuantity: number;
  reservedQuantity: number;
  availableQuantity: number;
}

export type InventoryPageResponse = PageResponse<InventoryResponse>;

export interface InventoriesResponse {
  items: InventoryResponse[];
}

export type ReservationStatus = 'PENDING' | 'COMMITTED' | 'RELEASED' | 'EXPIRED';

export interface ReservationResponse {
  reservationId: Uuid;
  referenceId: string;
  skuId: number;
  quantity: number;
  status: ReservationStatus;
  expiresAt: IsoInstant;
}

export interface ReservationListResponse {
  reservations: ReservationResponse[];
}
