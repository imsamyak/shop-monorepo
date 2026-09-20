import { useCase } from '@shop/sdk-core';
import {
  adjustStockBody,
  getInventoriesQuery,
  listInventoryQuery,
  listReservationsQuery,
  reservationParams,
  skuParams,
  stockQuantityBody,
} from './schemas.js';
import type {
  InventoriesResponse,
  InventoryPageResponse,
  InventoryResponse,
  ReservationListResponse,
  ReservationResponse,
} from './types.js';

export type * from './types.js';

export const getInventory = useCase('GetInventory')
  .service('inventory')
  .get('/inventory/{skuId}')
  .params(skuParams)
  .returns<InventoryResponse>()
  .authRequired()
  .build();

/** Paged, sorted by SKU id. Same endpoint as `GetInventories`, which passes `skuIds`. */
export const listInventory = useCase('ListInventory')
  .service('inventory')
  .get('/inventory')
  .query(listInventoryQuery)
  .returns<InventoryPageResponse>()
  .authRequired()
  .build();

/** Bulk lookup, unpaged. Same endpoint as `ListInventory`; `skuIds` is what selects this behaviour. */
export const getInventories = useCase('GetInventories')
  .service('inventory')
  .get('/inventory')
  .query(getInventoriesQuery)
  .returns<InventoriesResponse>()
  .authRequired()
  .build();

export const addStock = useCase('AddStock')
  .service('inventory')
  .post('/inventory/{skuId}/stock/add')
  .params(skuParams)
  .body(stockQuantityBody)
  .returns<InventoryResponse>()
  .authRequired()
  .build();

export const removeStock = useCase('RemoveStock')
  .service('inventory')
  .post('/inventory/{skuId}/stock/remove')
  .params(skuParams)
  .body(stockQuantityBody)
  .returns<InventoryResponse>()
  .authRequired()
  .build();

/** Sets the total quantity on hand. */
export const adjustStock = useCase('AdjustStock')
  .service('inventory')
  .post('/inventory/{skuId}/stock')
  .params(skuParams)
  .body(adjustStockBody)
  .returns<InventoryResponse>()
  .authRequired()
  .build();

export const getReservation = useCase('GetReservation')
  .service('inventory')
  .get('/inventory/reservations/{reservationId}')
  .params(reservationParams)
  .returns<ReservationResponse>()
  .authRequired()
  .build();

export const listReservations = useCase('ListReservations')
  .service('inventory')
  .get('/inventory/reservations')
  .query(listReservationsQuery)
  .returns<ReservationListResponse>()
  .authRequired()
  .build();

export const inventoryUseCases = [
  getInventory,
  listInventory,
  getInventories,
  addStock,
  removeStock,
  adjustStock,
  getReservation,
  listReservations,
] as const;
