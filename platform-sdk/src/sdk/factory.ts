import {
  assetUseCases,
  cartUseCases,
  catalogUseCases,
  checkoutUseCases,
  identityUseCases,
  inventoryUseCases,
  listingUseCases,
  orderUseCases,
} from '../usecases/index.js';
import { SdkBuilder } from './builder.js';
import type { SdkOptions } from './sdk.js';

/** A builder with every ecom use case in its gateway; chain more `.addGateway(...)` or call `.build()`. */
export function ecomSdkBuilder<Ctx = undefined>(options: SdkOptions<Ctx>) {
  return SdkBuilder.create(options)
    .addGateway('auth', identityUseCases)
    .addGateway('catalog', catalogUseCases)
    .addGateway('products', listingUseCases)
    .addGateway('cart', cartUseCases)
    .addGateway('checkout', checkoutUseCases)
    .addGateway('orders', orderUseCases)
    .addGateway('inventory', inventoryUseCases)
    .addGateway('assets', assetUseCases);
}

/** The SDK with every ecom use case: `const { auth, cart } = sdk.with(event)`. */
export function createEcomSdk<Ctx = undefined>(options: SdkOptions<Ctx>) {
  return ecomSdkBuilder(options).build();
}

export type EcomSdk<Ctx = undefined> = ReturnType<typeof createEcomSdk<Ctx>>;

/** The gateways `sdk.with(ctx)` returns. */
export type EcomGateways<Ctx = undefined> = ReturnType<EcomSdk<Ctx>['with']>;
