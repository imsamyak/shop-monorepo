import 'server-only';
import { SdkAuthError, SdkHttpError, type CartResponse, type CatalogProduct, type UserResponse } from '@shop/platform-sdk';
import { errorMessage } from './errors';
import { mainImage } from './images';
import { getSkuIndex, skuLabel } from './catalog';
import { api } from './sdk';
import { getUser } from './session';
import type { CartView, ProductCard } from './types';

function failure(error: unknown) {
  if (error instanceof SdkAuthError) return Response.json({ error: 'Sign in to continue.' }, { status: 401 });
  // a 401 from a service is a bad token, which the buyer fixes by signing in again
  if (error instanceof SdkHttpError && error.status === 401) {
    return Response.json({ error: 'Sign in to continue.' }, { status: 401 });
  }
  const status = error instanceof SdkHttpError && error.status < 500 ? error.status : 502;
  // errorMessage words a 401 as a wrong password, which does not fit here
  return Response.json({ error: errorMessage(error) }, { status });
}

/** An answer for a signed-in buyer; anyone else gets 401. */
export async function forUser(handler: (user: UserResponse) => Promise<unknown>): Promise<Response> {
  try {
    const user = await getUser();
    if (!user) return Response.json({ error: 'Sign in to continue.' }, { status: 401 });
    return Response.json(await handler(user));
  } catch (error) {
    return failure(error);
  }
}

/** An answer anyone may have. */
export async function forAnyone(handler: () => Promise<unknown>): Promise<Response> {
  try {
    return Response.json(await handler());
  } catch (error) {
    return failure(error);
  }
}

export function toCard(product: CatalogProduct): ProductCard {
  const price = product.lowestSkuPrice ?? product.price;
  return {
    id: product.id,
    title: product.title,
    image: mainImage(product.images),
    price,
    from: product.lowestSkuPrice !== null && product.lowestSkuPrice < product.price,
    available: product.availability === 'AVAILABLE',
  };
}

/** The cart with names, pictures and prices from the catalog, which the cart service does not keep. */
export async function toCartView(cart: CartResponse): Promise<CartView> {
  const index = await getSkuIndex();
  const items = cart.items.map((item) => {
    const info = index.get(item.skuId);
    return {
      skuId: item.skuId,
      quantity: item.quantity,
      productId: info?.product.id ?? null,
      title: info?.product.title ?? `Item ${item.skuId}`,
      image: mainImage(info?.product.images),
      label: info ? skuLabel(info.sku) : '',
      unitPrice: info?.sku.price ?? 0,
      available: info ? info.sku.availability === 'AVAILABLE' : false,
    };
  });
  return {
    version: cart.version,
    count: items.reduce((sum, item) => sum + item.quantity, 0),
    total: items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0),
    items,
  };
}

export const loadCart = async (customerId: string) => toCartView(await api().cart.getCart({ customerId }));
