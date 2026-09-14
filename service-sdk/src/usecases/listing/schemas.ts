import { z } from 'zod';
import { id, money, page, requiredText, size, uuid, version } from '../shared/schemas.js';

export const productParams = z.strictObject({ id: uuid });
export const skusParams = z.strictObject({ productId: uuid });
export const skuParams = z.strictObject({ productId: uuid, skuId: id });

export const versionQuery = z.strictObject({ version });

const title = requiredText(50, 5);
const skuPrice = money(0);
const properties = z.record(z.string(), z.string());

/** Addresses returned by the asset service, in display order: the first is the main picture. At most 10, no repeats. */
const images = z.array(z.url().max(500)).max(10);

export const createProductBody = z.strictObject({
  title,
  description: requiredText(200),
  defaultPrice: money(1),
  images: images.optional(),
});

/** `title` matches products whose title contains it. */
export const listProductsQuery = z.strictObject({
  sellerId: uuid.optional(),
  title: z.string().optional(),
  page: page.optional(),
  size: size.optional(),
  sort: z.enum(['createdAt,desc', 'createdAt,asc', 'createdAt', 'title,asc', 'title', 'title,desc']).optional(),
});

/** Shared by PATCH and PUT: `description` or `images` left out keeps it, `null` clears it, a list replaces it. */
export const updateProductBody = z.strictObject({
  version,
  title,
  description: z.string().max(200).nullable().optional(),
  images: images.nullable().optional(),
});

/** Without `price` the SKU takes the product's default price. */
export const createSkuBody = z.strictObject({
  price: skuPrice.optional(),
  properties: properties.optional(),
});

export const updateSkuBody = z.strictObject({
  version,
  price: skuPrice.optional(),
  properties: z
    .strictObject({
      add: properties.optional(),
      remove: z.array(z.string()).optional(),
    })
    .optional(),
});
