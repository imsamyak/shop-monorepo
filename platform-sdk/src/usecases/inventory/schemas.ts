import { z } from 'zod';
import { id, page, requiredText, size, uuid } from '../shared/schemas.js';

export const skuParams = z.strictObject({ skuId: id });
export const reservationParams = z.strictObject({ reservationId: uuid });

export const listInventoryQuery = z.strictObject({ page: page.optional(), size: size.optional() });

export const getInventoriesQuery = z.strictObject({ skuIds: z.array(id).min(1).max(100) });

/** Quantity added or removed in one call. */
export const stockQuantityBody = z.strictObject({ quantity: z.number().int().positive().max(1_000_000) });

export const adjustStockBody = z.strictObject({ totalQuantity: z.number().int().nonnegative() });

export const listReservationsQuery = z.strictObject({
  checkoutId: requiredText(100),
  status: z.enum(['PENDING', 'COMMITTED', 'RELEASED', 'EXPIRED']).optional(),
});
