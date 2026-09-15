import { useCase } from '../../core/use-case-builder.js';
import { listOrdersQuery, orderParams } from './schemas.js';
import type { OrderResponse, OrdersResponse } from './types.js';

export type * from './types.js';

export const getOrder = useCase('GetOrder')
  .service('order')
  .get('/orders/{orderId}')
  .params(orderParams)
  .returns<OrderResponse>()
  .build();

export const listOrders = useCase('ListOrders')
  .service('order')
  .get('/orders')
  .query(listOrdersQuery)
  .returns<OrdersResponse>()
  .build();

export const orderUseCases = [getOrder, listOrders] as const;
