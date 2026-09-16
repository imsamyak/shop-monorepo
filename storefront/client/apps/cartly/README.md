# Cartly

The customer storefront, on Next.js (App Router) and `@shop/platform-sdk`. Buyers browse the catalog, open a product,
pick an option, sign up or sign in, manage a cart, check out (draft, then place the order or cancel) and see their orders.

## Run

From the workspace root (`shop-monorepo`), with the platform services running:

```bash
pnpm install
pnpm --filter @cartly/web dev      # builds the SDK first, then starts http://localhost:3000
```

The service URLs come from the SDK's variables (`ECOM_API_URL` or `ECOM_<SERVICE>_URL`) and default to the platform's
local ports. See `.env.example`.

## How it fits together

- `lib/sdk.ts` creates the SDK once; the access token lives in an httpOnly `access_token` cookie via `cookies()`.
- All SDK calls run on the server: pages are server components, and changes go through server actions in
  `lib/actions.ts`. The browser never holds the token.
- `lib/session.ts` resolves the signed-in user with `auth.getCurrentUser()`; the user's id is the cart's `customerId`.
- A product's `images` are in the order the seller gave them; the first is the main picture (`lib/images.ts`).
- Carts and orders only hold SKU ids, so `lib/catalog.ts` looks names and pictures up in the catalog's newest 100
  products.
