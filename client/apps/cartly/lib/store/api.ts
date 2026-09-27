import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { CartView, ProductPage, ProductQuery } from '../types';

/** The server answers `{ error }` with a status; this is the message for the buyer. */
export function apiError(error: unknown): string {
  const data = (error as { data?: { error?: string } } | undefined)?.data;
  return data?.error ?? 'Something went wrong. Please try again.';
}

export const isUnauthorized = (error: unknown) => (error as { status?: number } | undefined)?.status === 401;

/**
 * Everything the browser asks the shop for goes through here and is cached: product lists by their filters, and the cart.
 * The token stays in an httpOnly cookie, so the requests are to this app's own `/api` routes, which add it.
 */
export const shopApi = createApi({
  reducerPath: 'shopApi',
  baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
  tagTypes: ['Cart'],
  endpoints: (build) => ({
    listProducts: build.query<ProductPage, ProductQuery>({
      query: ({ search, sort, inStock, page, size }) => ({
        url: 'products',
        params: { sort, page, size, ...(search ? { search } : {}), ...(inStock ? { inStock: '1' } : {}) },
      }),
    }),

    getCart: build.query<CartView, void>({
      query: () => 'cart',
      providesTags: ['Cart'],
    }),

    addToCart: build.mutation<CartView, { skuId: number; quantity: number }>({
      query: (body) => ({ url: 'cart/items', method: 'POST', body }),
      async onQueryStarted(_, { dispatch, queryFulfilled }) {
        try {
          const { data } = await queryFulfilled;
          dispatch(shopApi.util.upsertQueryData('getCart', undefined, data));
        } catch {
          // the caller shows the error
        }
      },
    }),

    setCartQuantity: build.mutation<CartView, { skuId: number; quantity: number; version: number }>({
      query: ({ skuId, ...body }) => ({ url: `cart/items/${skuId}`, method: 'PATCH', body }),
      // the line changes at once and goes back if the server says no
      async onQueryStarted({ skuId, quantity }, { dispatch, queryFulfilled }) {
        const patch = dispatch(
          shopApi.util.updateQueryData('getCart', undefined, (cart) => {
            const line = cart.items.find((item) => item.skuId === skuId);
            if (!line) return;
            cart.count += quantity - line.quantity;
            cart.total += (quantity - line.quantity) * line.unitPrice;
            line.quantity = quantity;
          }),
        );
        try {
          const { data } = await queryFulfilled;
          dispatch(shopApi.util.upsertQueryData('getCart', undefined, data));
        } catch {
          patch.undo();
        }
      },
    }),

    removeCartItem: build.mutation<CartView, { skuId: number; version: number }>({
      query: ({ skuId, version }) => ({ url: `cart/items/${skuId}`, method: 'DELETE', params: { version } }),
      async onQueryStarted({ skuId }, { dispatch, queryFulfilled }) {
        const patch = dispatch(
          shopApi.util.updateQueryData('getCart', undefined, (cart) => {
            const line = cart.items.find((item) => item.skuId === skuId);
            if (!line) return;
            cart.count -= line.quantity;
            cart.total -= line.quantity * line.unitPrice;
            cart.items = cart.items.filter((item) => item.skuId !== skuId);
          }),
        );
        try {
          const { data } = await queryFulfilled;
          dispatch(shopApi.util.upsertQueryData('getCart', undefined, data));
        } catch {
          patch.undo();
        }
      },
    }),
  }),
});

export const {
  useListProductsQuery,
  useGetCartQuery,
  useAddToCartMutation,
  useSetCartQuantityMutation,
  useRemoveCartItemMutation,
  usePrefetch,
} = shopApi;
