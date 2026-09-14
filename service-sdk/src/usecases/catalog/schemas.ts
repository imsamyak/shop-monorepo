import { z } from 'zod';
import { page, size } from '../shared/schemas.js';

export const productParams = z.strictObject({ id: z.string().min(1) });

export const listProductsQuery = z.strictObject({
  page: page.optional(),
  size: size.optional(),
  sort: z.enum(['createdAt,desc', 'createdAt,asc', 'price,asc', 'price,desc']).optional(),
  availability: z.enum(['AVAILABLE', 'OUT_OF_STOCK']).optional(),
});
