import { z } from 'zod';
import { id, uuid, version } from '../shared/schemas.js';

/** Quantity of one cart or checkout line. */
const quantity = z.number().int().positive().max(1000);

export const cartParams = z.strictObject({ customerId: uuid });
export const cartItemParams = z.strictObject({ customerId: uuid, skuId: id });
export const checkoutParams = z.strictObject({ checkoutId: uuid });

/** The cart version the caller last saw; a different current one answers 409. */
export const cartVersionQuery = z.strictObject({ version: version.optional() });

export const addCartItemBody = z.strictObject({ skuId: id, quantity, version: version.optional() });

export const setCartItemBody = z.strictObject({ quantity, version: version.optional() });

/** The customer is the signed-in user. Leaving `items` out (or empty) drafts that customer's current cart. */
export const draftCheckoutBody = z.strictObject({
  items: z
    .array(z.strictObject({ skuId: id, quantity }))
    .max(50)
    .optional(),
  ttlSeconds: z.number().int().positive().max(3600).optional(),
});
