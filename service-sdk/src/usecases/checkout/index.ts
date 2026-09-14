import { useCase } from '../../core/use-case-builder.js';
import {
  addCartItemBody,
  cartItemParams,
  cartParams,
  cartVersionQuery,
  checkoutParams,
  draftCheckoutBody,
  setCartItemBody,
} from './schemas.js';
import type { CartResponse, CheckoutResponse } from './types.js';

export type * from './types.js';

export const getCart = useCase('GetCart')
  .service('checkout')
  .get('/carts/{customerId}')
  .params(cartParams)
  .returns<CartResponse>()
  .build();

/** Adds to the quantity of an existing line. */
export const addCartItem = useCase('AddCartItem')
  .service('checkout')
  .post('/carts/{customerId}/items')
  .params(cartParams)
  .body(addCartItemBody)
  .returns<CartResponse>()
  .build();

export const setCartItem = useCase('SetCartItem')
  .service('checkout')
  .put('/carts/{customerId}/items/{skuId}')
  .params(cartItemParams)
  .body(setCartItemBody)
  .returns<CartResponse>()
  .build();

export const removeCartItem = useCase('RemoveCartItem')
  .service('checkout')
  .delete('/carts/{customerId}/items/{skuId}')
  .params(cartItemParams)
  .query(cartVersionQuery)
  .returns<CartResponse>()
  .build();

export const clearCart = useCase('ClearCart')
  .service('checkout')
  .delete('/carts/{customerId}')
  .params(cartParams)
  .query(cartVersionQuery)
  .returns<CartResponse>()
  .build();

/** Prices and sellers are looked up server-side. */
export const draftCheckout = useCase('DraftCheckout')
  .service('checkout')
  .post('/checkouts')
  .body(draftCheckoutBody)
  .returns<CheckoutResponse>()
  .build();

export const getCheckout = useCase('GetCheckout')
  .service('checkout')
  .get('/checkouts/{checkoutId}')
  .params(checkoutParams)
  .returns<CheckoutResponse>()
  .build();

export const commitCheckout = useCase('CommitCheckout')
  .service('checkout')
  .post('/checkouts/{checkoutId}/commit')
  .params(checkoutParams)
  .returns<CheckoutResponse>()
  .build();

export const cancelCheckout = useCase('CancelCheckout')
  .service('checkout')
  .post('/checkouts/{checkoutId}/cancel')
  .params(checkoutParams)
  .returns<CheckoutResponse>()
  .build();

export const cartUseCases = [getCart, addCartItem, setCartItem, removeCartItem, clearCart] as const;

export const checkoutUseCases = [draftCheckout, getCheckout, commitCheckout, cancelCheckout] as const;
