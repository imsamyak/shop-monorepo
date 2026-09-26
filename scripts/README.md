# scripts

Development scripts. They are run by hand when needed and are not part of any start-up or deployment.

## `pnpm ingest`

Resets the local shop and fills it with demo data:

1. removes every seller, and every listing, stock record, catalog entry, cart, checkout and order (buyer accounts stay)
2. empties the local S3 bucket (S3Mock at `localhost:9090`)
3. creates one seller (`seller@cartly.test`, password in the script's output) with 50 products and 200 SKUs, real
   photos uploaded through the asset service, and stock

It needs the local resources up (`server/platform/bootstrap/local/local.ps1 up`) and the identity, listing, inventory,
catalog and asset services running. Product data and photos come from dummyjson.com and are cached in `.cache/`.

It refuses to run when a service address in the environment points anywhere but localhost. Options (environment):
`SEED_PRODUCTS`, `SEED_SKUS_PER_PRODUCT`, `SEED_SELLER_EMAIL`, `SEED_SELLER_PASSWORD`.
