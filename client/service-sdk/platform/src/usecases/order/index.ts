import { useCase } from '@shop/sdk-core';
import { listOrdersQuery, orderParams } from './schemas.js';
import type { OrderResponse, OrdersResponse } from './types.js';

export type * from './types.js';

export const getOrder = useCase('GetOrder')
  .service('order')
  .get('/orders/{orderId}')
  .params(orderParams)
  .returns<OrderResponse>()
  .authRequired()
  .build();

export const listOrders = useCase('ListOrders')
  .service('order')
  .get('/orders')
  .query(listOrdersQuery)
  .returns<OrdersResponse>()
  .authRequired()
  .build();

export const orderUseCases = [getOrder, listOrders] as const;
