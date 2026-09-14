import { assetUseCases } from './asset/index.js';
import { catalogUseCases } from './catalog/index.js';
import { cartUseCases, checkoutUseCases } from './checkout/index.js';
import { identityUseCases } from './identity/index.js';
import { inventoryUseCases } from './inventory/index.js';
import { listingUseCases } from './listing/index.js';
import { orderUseCases } from './order/index.js';

export * from './asset/index.js';
export * from './catalog/index.js';
export * from './checkout/index.js';
export * from './identity/index.js';
export * from './inventory/index.js';
export * from './listing/index.js';
export * from './order/index.js';
export type * from './shared/types.js';

/** Every use case. Each list lives at the bottom of its service's `index.ts`. */
export const allUseCases = [
  ...identityUseCases,
  ...catalogUseCases,
  ...listingUseCases,
  ...cartUseCases,
  ...checkoutUseCases,
  ...orderUseCases,
  ...inventoryUseCases,
  ...assetUseCases,
] as const;
