import { expectTypeOf } from 'vitest';
import {
  type addCartItem,
  type CartResponse,
  createEcomSdk,
  type SessionResponse,
  type UseCaseRequest,
  type UserResponse,
} from '../src/index.js';
import type { Ctx } from './helpers.js';

/** Compile-time checks only (vitest does not pick this file up): `npm run typecheck` fails if any stop holding. */

declare const sdk: ReturnType<typeof createEcomSdk<Ctx>>;
declare const anonymous: ReturnType<typeof createEcomSdk>;
declare const ctx: Ctx;

export function gatewayMethodsAreTyped() {
  const { auth, cart } = sdk.with(ctx);
  expectTypeOf(auth.login({ email: '', password: '' })).resolves.toEqualTypeOf<SessionResponse>();
  expectTypeOf(auth.getCurrentUser()).resolves.toEqualTypeOf<UserResponse>();
  expectTypeOf(cart.getCart({ customerId: '' })).resolves.toEqualTypeOf<CartResponse>();
  expectTypeOf(auth.logout()).resolves.toBeVoid();
}

/** Params, query and body fields merged into one object. */
export function requestIsOneFlatObject() {
  expectTypeOf<UseCaseRequest<typeof addCartItem>>().toEqualTypeOf<{
    customerId: string;
    skuId: number;
    quantity: number;
    version?: number | undefined;
  }>();
}

export function wrongInputDoesNotCompile() {
  const { auth, catalog, inventory } = sdk.with(ctx);
  // @ts-expect-error password missing
  void auth.login({ email: '' });
  // @ts-expect-error request required
  void auth.login();
  // @ts-expect-error sort is an enum
  void catalog.listCatalogProducts({ sort: 'newest' });
  // @ts-expect-error skuId is a number
  void inventory.getInventory({ skuId: '1' });
  // @ts-expect-error no such method
  void auth.loginWithGoogle();
  // @ts-expect-error unknown use case
  void sdk.execute('Nope', {}, ctx);
  // @ts-expect-error context is required when Ctx does not allow undefined
  void sdk.with();
}

export function executeStillWorks() {
  expectTypeOf(sdk.execute('GetCart', { customerId: '' }, ctx)).resolves.toEqualTypeOf<CartResponse>();
  void sdk.execute('ListOrders', undefined, ctx);
  void anonymous.execute('ListOrders');
}

export function contextIsOptionalWithoutOne() {
  void anonymous.with().orders.listOrders();
}
