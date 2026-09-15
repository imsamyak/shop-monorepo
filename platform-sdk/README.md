# @shop/platform-sdk

Typed TypeScript client for the shop platform (identity, catalog, listing, checkout, order, inventory, asset). Every endpoint
is a **use case**: a small object with its service, method, path, validators for params / query / body, and whether it
needs an access token. The SDK is built from use cases with a builder.

## Usage

Create the SDK once. The token store provider is called on every call with that call's context, so each user's token
can live in their own cookie. With Nuxt, the context is the `H3Event`:

```ts
// server/utils/sdk.ts
import { createEcomSdk } from '@shop/platform-sdk';
import { type H3Event, getCookie, setCookie, deleteCookie } from 'h3';

export const sdk = createEcomSdk<H3Event>({
  tokenStore: (event) => ({
    getAccessToken: () => getCookie(event, 'access_token'),
    setAccessToken: (token) =>
      token ? setCookie(event, 'access_token', token, { httpOnly: true, path: '/' }) : deleteCookie(event, 'access_token'),
  }),
});

// server/api/cart.post.ts
export default defineEventHandler(async (event) => {
  const { auth, cart } = sdk.with(event);
  const me = await auth.getCurrentUser();                                // sends the token
  return cart.addCartItem({ customerId: me.id, skuId: 42, quantity: 1 }); // CartResponse
});
```

`sdk.with(ctx)` returns the gateways: `auth`, `catalog`, `products`, `cart`, `checkout`, `orders`, `inventory`, `assets`.
Each has one method per use case, named after it (`AddCartItem` → `addCartItem`). `auth.login` and
`auth.registerBuyer` store the token and `auth.logout` clears it. When the token store needs no context (Next.js
`cookies()`), leave the generic out and call `sdk.with()`.

A request is one flat object. The SDK puts fields named by a path placeholder (`{customerId}`) into the URL, and every
other field into the query string on GET/DELETE or into the JSON body otherwise.

**Images.** Upload files with `assets.uploadImages({ files })` (`files` are `Blob`s or `File`s, at most 10, in the order you
want them shown) and put the returned `url`s in a product's `images`. The order of `images` is kept all the way to the
catalog, and the first is the main picture. Sellers can sign up with `auth.registerSeller`.

```ts
const { assets, products } = sdk.with(event);
const { items } = await assets.uploadImages({ files: [front, back] });          // [{ key, url, ... }, ...] in order
await products.createProduct({ title, description, defaultPrice: 10, images: items.map((i) => i.url) });
```

`sdk.execute('AddCartItem', request, ctx)` reaches the same use cases by name, which is handy for generic code.
Requests, responses and names are type-checked either way.

Make your own gateways with the builder:

```ts
import { SdkBuilder, cartUseCases, getOrder } from '@shop/platform-sdk';

const sdk = SdkBuilder.create<H3Event>({ tokenStore })
  .addGateway('basket', cartUseCases)
  .addGateway('history', [getOrder])
  .build();

const { basket } = sdk.with(event);
```

`ecomSdkBuilder(options)` is the default set as a builder, so you can chain more `.addGateway(...)` calls before
`.build()`.

## Base URLs

Read from the environment when `build()` runs, in this order:

1. `options.baseUrls.<service>`
2. `ECOM_<SERVICE>_URL`: `ECOM_IDENTITY_URL`, `ECOM_CATALOG_URL`, `ECOM_LISTING_URL`, `ECOM_CHECKOUT_URL`,
   `ECOM_ORDER_URL`, `ECOM_INVENTORY_URL`, `ECOM_ASSET_URL`
3. `ECOM_API_URL`, one URL for all services (e.g. behind a gateway)
4. The local ports from the services' `application.yml` (`http://localhost:8081` … `8087`)

## Validation

Params and query are always fully validated because they build the URL. Bodies follow `options.validation`:

| Mode | Checks |
|---|---|
| `shape` (default) | required fields, types, formats, unknown keys; numeric and length limits are left to the backend |
| `strict` | everything, limits included |
| `off` | nothing; the body is sent as given |

Business rules (stock, versions, expiry, duplicates) are only checked by the backend.

## Errors

| Error | When |
|---|---|
| `SdkValidationError` | input failed a validator; `part` and `invalidFields` (`{ "items[0].quantity": "..." }`, same shape as the backend's 400) |
| `SdkAuthError` | the use case needs a token and the store had none |
| `SdkHttpError` | non-2xx answer; `status`, `errorCode`, `invalidFields`, `body` |
| `SdkNetworkError` | no response at all |

None but `SdkHttpError` reach the backend.

## Layout

```
src/
  core/        use case types, use case builder, validation, URL building, errors, token store
  config/      base URLs from env
  sdk/         Sdk (with, execute), SdkBuilder, createEcomSdk factory and its gateways
  usecases/
    shared/    schemas and types used by several services
    <service>/
      index.ts    the service's use cases and their lists
      schemas.ts  zod schemas for params, query and body
      types.ts    response types
    index.ts   allUseCases
```

## Adding a use case

1. Add its schemas to `src/usecases/<service>/schemas.ts` and its response type to `types.ts` if new.
2. Declare it in `src/usecases/<service>/index.ts`:

   ```ts
   export const removeCartItem = useCase('RemoveCartItem')
     .service('checkout')
     .delete('/carts/{customerId}/items/{skuId}')
     .params(cartItemParams)
     .query(cartVersionQuery)
     .returns<CartResponse>()
     .build();
   ```

   Also available: `.body(schema)`, `.authRequired()`, `.storesToken()`, `.clearsToken()`.
3. Add it to its list at the bottom of the same file (`cartUseCases`). The list is a gateway in
   `sdk/factory.ts`, so the use case appears as `cart.removeCartItem(...)`.

A new list or service also needs its gateway in `sdk/factory.ts` and its spread in `allUseCases`. A new service also
needs its name in `ServiceName` (`core/types.ts`) and an env variable and default in `config/env.ts`.

**Removing one** is the reverse: delete the declaration and its entry in the list.

`npm test` guards these rules:
- names are unique
- params validators match the path placeholders
- GET/DELETE take no body and other methods no query
- no field name appears in two parts
- every exported use case is in a list

## Scripts

```bash
npm run typecheck   # includes compile-time type tests in tests/types.check.ts
npm test
npm run build       # ESM + .d.ts into dist/
```
