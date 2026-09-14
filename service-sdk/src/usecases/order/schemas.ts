import { z } from 'zod';
import { page, size, uuid } from '../shared/schemas.js';

export const orderParams = z.strictObject({ orderId: uuid });

export const listOrdersQuery = z.strictObject({
  customerId: uuid.optional(),
  status: z.enum(['DRAFT', 'COMMITTED', 'CANCELLED']).optional(),
  page: page.optional(),
  size: size.optional(),
});
