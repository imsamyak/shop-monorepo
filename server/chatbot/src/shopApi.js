// Talks to the platform services through @shop/platform-sdk, as the signed-in customer.
//
//   import { shopApi } from "./shopApi.js";
//   await shopApi(token).getCart(customerId);
//
// Every call carries the caller's own access token, so the services decide what that customer may see.

import { createEcomSdk } from "@shop/platform-sdk";

// The context of a call is the caller's token; the SDK asks for it on every request.
const sdk = createEcomSdk({
  tokenStore: (token) => ({ getAccessToken: () => token, setAccessToken: () => {} }),
});

export function shopApi(token) {
  const { products, catalog, cart, orders } = sdk.with(token);
  return {
    // Search products by title (public)
    searchProducts: (title) => products.listProducts({ title: title || undefined, size: 5 }),

    // Full product details: price, stock availability, SKUs (public)
    getProduct: (productId) => catalog.getCatalogProduct({ id: productId }),

    // Cart
    getCart: (customerId) => cart.getCart({ customerId }),
    addToCart: (customerId, skuId, quantity) => cart.addCartItem({ customerId, skuId, quantity }),

    // Orders
    listOrders: (customerId, status) => orders.listOrders({ customerId, status, size: 5 }),
    getOrder: (orderId) => orders.getOrder({ orderId }),
  };
}
