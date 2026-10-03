// Seeds the development shop: empties it (lib/reset.mjs), then creates the sellers in resources/sellers.json and the products
// in resources/products.json (each with its SKUs, photos and stock). The photos are in resources/images.
//
//   pnpm ingest                  resets the development shop first
//   pnpm ingest -- --seed-only   only creates (for a new, empty environment)
//
// Needs the local resources (server/platform/bootstrap/local/local.ps1 up) and the identity, listing, inventory, catalog and
// asset services running. The AWS keys and region come from the repo-root .env. It deletes data, so it is run by hand.

import { createEcomSdk } from '@shop/platform-sdk';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { reset } from './lib/reset.mjs';
import { seed } from './lib/seed.mjs';

const data = (name) => JSON.parse(readFileSync(join(import.meta.dirname, '../../resources', name), 'utf8'));
const log = (...args) => console.log(...args);
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function main() {
  const started = Date.now();
  const sellers = data('sellers.json');
  const products = data('products.json');

  const unknown = products.filter((product) => !sellers.some((seller) => seller.email === product.seller));
  if (unknown.length) throw new Error(`${unknown.length} product(s) name a seller that is not in sellers.json`);

  // a new environment has nothing to reset, and the reset needs the local containers, so `--seed-only` skips it
  if (process.argv.includes('--seed-only')) log('Seeding only (nothing is reset)');
  else await reset(log);

  log(`Creating ${sellers.length} seller(s) and ${products.length} product(s)`);
  const created = await seed(sellers, products, log);

  // the catalog is filled from events; wait until it has caught up
  log('Waiting for the catalog');
  const api = createEcomSdk({ tokenStore: () => ({ getAccessToken: () => undefined, setAccessToken: () => {} }) }).with();
  let listed = 0;
  for (let attempt = 0; attempt < 60 && listed < created.products; attempt++) {
    listed = (await api.catalog.listCatalogProducts({ size: 1 })).totalElements;
    if (listed < created.products) await sleep(1000);
  }
  if (listed < created.products) throw new Error(`The catalog lists ${listed} of ${created.products} products`);
  const available = (await api.catalog.listCatalogProducts({ size: 1, availability: 'AVAILABLE' })).totalElements;

  log(`Done in ${Math.round((Date.now() - started) / 1000)}s: ${listed} products, ${created.skus} SKUs, ${available} in stock.`);
  for (const seller of sellers) log(`Seller sign-in: ${seller.email} / ${seller.password}`);
}

main().catch((error) => {
  console.error('\nIngestion failed:', error.message ?? error);
  process.exit(1);
});
