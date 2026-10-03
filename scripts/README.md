# scripts

Development scripts. They are run by hand when needed and are not part of any start-up or deployment.

## `pnpm ingest`

Resets the development shop and fills it with demo data from `resources/` (`sellers.json`, `products.json` and the photos in `images/`):

1. removes every seller, and every listing, stock record, catalog entry, cart, checkout and order (buyer accounts stay)
2. deletes every object under `images/` in the S3 bucket the asset service writes to (`imsamyak.shop-asset-dev`, from `ASSETS_BUCKET`; the AWS keys and region come from the repo-root `.env`)
3. creates the sellers and their products (50 products, 200 SKUs), uploads the photos through the asset service, and sets stock

It needs the local resources up (`server/platform/bootstrap/local/local.ps1 up`) and the identity, listing, inventory,
catalog and asset services running. The photos are committed in `resources/images/` (about 8 MB), so it runs offline.

It refuses to run when a service address in the environment points anywhere but localhost. Options (environment):
`SEED_PRODUCTS`, `SEED_SKUS_PER_PRODUCT`, `SEED_SELLER_EMAIL`, `SEED_SELLER_PASSWORD`.
