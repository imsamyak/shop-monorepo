// Seeds the local shop: removes every seller and everything built on their listings, empties the local S3 bucket,
// then creates one seller with 50 products (200 SKUs) that have real photos, and stocks them.
//
//   pnpm ingest
//
// Needs the local resources (server/platform/bootstrap/local/local.ps1 up) and these services running: identity,
// listing, inventory, catalog and asset. Product data and photos come from dummyjson.com.
//
// What it deletes (local development data only; it refuses to run against anything but localhost):
//   - sellers (identity_db.users with ROLE_SELLER) and their reset tokens
//   - all listings, stock and catalog entries (listing_db, inventory_db, Mongo "catalog")
//   - carts, checkouts and orders, because they point at SKUs that no longer exist
//   - every object in the local S3 bucket (S3Mock at localhost:9090)
// Buyer accounts are kept.

import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { createEcomSdk } from '@shop/platform-sdk';

const PRODUCTS = Number(process.env.SEED_PRODUCTS ?? 50);
const SKUS_PER_PRODUCT = Number(process.env.SEED_SKUS_PER_PRODUCT ?? 4);
const IMAGES_PER_PRODUCT = 3;
const USD_TO_INR = 83;
const CONCURRENCY = 3;

const SELLER = {
  email: process.env.SEED_SELLER_EMAIL ?? 'seller@cartly.test',
  password: process.env.SEED_SELLER_PASSWORD ?? 'Seller#2026pass',
};

const S3_URL = process.env.SEED_S3_URL ?? 'http://localhost:9090';
const S3_BUCKET = process.env.SEED_S3_BUCKET ?? 'ecom-assets';
const PG_CONTAINER = process.env.SEED_PG_CONTAINER ?? 'ecom-local-postgres-1';
const MONGO_CONTAINER = process.env.SEED_MONGO_CONTAINER ?? 'ecom-local-mongo-1';

const CACHE = join(import.meta.dirname, '.cache');
mkdirSync(CACHE, { recursive: true });

const log = (...args) => console.log(...args);
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// ---------------------------------------------------------------------------------------------------- the reset

function assertLocal() {
  const remote = ['ECOM_API_URL', 'ECOM_IDENTITY_URL', 'ECOM_LISTING_URL', 'ECOM_ASSET_URL', 'ECOM_INVENTORY_URL']
    .map((name) => process.env[name])
    .filter((url) => url && !/^https?:\/\/(localhost|127\.0\.0\.1)(:|\/|$)/.test(url));
  if (remote.length || !/^https?:\/\/(localhost|127\.0\.0\.1)(:|\/|$)/.test(S3_URL)) {
    throw new Error(`Refusing to reset: this script only resets a local environment (${[...remote, S3_URL].join(', ')})`);
  }
}

function psql(database, sql) {
  return execFileSync('docker', ['exec', PG_CONTAINER, 'psql', '-U', 'ecom', '-d', database, '-v', 'ON_ERROR_STOP=1', '-Atc', sql], {
    encoding: 'utf8',
  }).trim();
}

function mongo(script) {
  return execFileSync('docker', ['exec', MONGO_CONTAINER, 'mongosh', '--quiet', '--eval', script], { encoding: 'utf8' }).trim();
}

async function clearBucket() {
  let removed = 0;
  for (;;) {
    const listing = await fetch(`${S3_URL}/${S3_BUCKET}?list-type=2&max-keys=1000`);
    if (!listing.ok) throw new Error(`Listing ${S3_BUCKET} failed: ${listing.status}`);
    const keys = [...(await listing.text()).matchAll(/<Key>([^<]+)<\/Key>/g)].map((m) => m[1]);
    if (keys.length === 0) return removed;
    for (let i = 0; i < keys.length; i += 20) {
      await Promise.all(
        keys.slice(i, i + 20).map((key) => fetch(`${S3_URL}/${S3_BUCKET}/${key.split('/').map(encodeURIComponent).join('/')}`, { method: 'DELETE' })),
      );
    }
    removed += keys.length;
  }
}

async function reset() {
  assertLocal();
  log('Resetting local data');

  const sellers = psql('identity_db', "select count(*) from users where role = 'ROLE_SELLER'");
  psql('identity_db', "delete from password_reset_tokens where user_id in (select id from users where role = 'ROLE_SELLER'); delete from users where role = 'ROLE_SELLER'");
  log(`  removed ${sellers} seller(s)`);

  psql('listing_db', 'truncate table outbox_events, product_images, product_skus, products restart identity cascade');
  psql('inventory_db', 'truncate table inbox, inventory_items, inventory_reservations, outbox_events restart identity cascade');
  psql('checkout_db', 'truncate table cart_items, carts, checkout_items, checkouts, inbox, outbox_events restart identity cascade');
  psql('order_db', 'truncate table inbox, order_items, orders restart identity cascade');
  log('  emptied listings, stock, carts, checkouts and orders');

  // documents only, so the collections and their indexes stay
  mongo("const d = db.getSiblingDB('catalog'); d.getCollectionNames().forEach((c) => d.getCollection(c).deleteMany({}))");
  log('  emptied the catalog');

  log(`  removed ${await clearBucket()} object(s) from s3://${S3_BUCKET}`);
}

// ---------------------------------------------------------------------------------------------------- the data

/** Fetches the whole dummyjson catalog once and keeps it next to the script. */
async function loadCatalog() {
  const file = join(CACHE, 'dummyjson-products.json');
  if (existsSync(file)) return JSON.parse(readFileSync(file, 'utf8'));
  const response = await fetch('https://dummyjson.com/products?limit=0&select=title,description,price,category,images');
  if (!response.ok) throw new Error(`dummyjson answered ${response.status}`);
  const { products } = await response.json();
  writeFileSync(file, JSON.stringify(products));
  return products;
}

/** Takes products round-robin across categories so the shop looks like a shop, not one aisle. */
function pick(products, count) {
  const byCategory = new Map();
  for (const product of products) {
    if (!product.images?.length) continue;
    if (!byCategory.has(product.category)) byCategory.set(product.category, []);
    byCategory.get(product.category).push(product);
  }
  const lists = [...byCategory.values()];
  const picked = [];
  for (let round = 0; picked.length < count && lists.some((list) => list[round]); round++) {
    for (const list of lists) if (list[round] && picked.length < count) picked.push(list[round]);
  }
  return picked;
}

const SIZES = ['S', 'M', 'L', 'XL'];
const SHOE_SIZES = ['6', '7', '8', '9', '10'];
const STORAGE = [
  ['64 GB', 0],
  ['128 GB', 0.1],
  ['256 GB', 0.22],
  ['512 GB', 0.4],
];
const COLORS = ['Black', 'White', 'Blue', 'Red', 'Green', 'Grey'];

/** The option a category is sold in, and what each value costs relative to the base price. */
function variantsFor(category, count) {
  if (/shirts|tops|dresses/.test(category)) return SIZES.slice(0, count).map((v, i) => ({ name: 'size', value: v, factor: 1 + i * 0.03 }));
  if (/shoes/.test(category)) return SHOE_SIZES.slice(0, count).map((v) => ({ name: 'size', value: `UK ${v}`, factor: 1 }));
  if (/smartphones|tablets|laptops/.test(category)) return STORAGE.slice(0, count).map(([v, f]) => ({ name: 'storage', value: v, factor: 1 + f }));
  if (/fragrances|beauty|skin-care/.test(category)) return ['30 ml', '50 ml', '100 ml', '200 ml'].slice(0, count).map((v, i) => ({ name: 'size', value: v, factor: 0.6 + i * 0.4 }));
  if (/groceries/.test(category)) return ['250 g', '500 g', '1 kg', '2 kg'].slice(0, count).map((v, i) => ({ name: 'pack', value: v, factor: 0.5 + i * 0.5 }));
  return COLORS.slice(0, count).map((v, i) => ({ name: 'color', value: v, factor: 1 + (i % 3) * 0.02 }));
}

/** At most `max` characters, cut at a word. */
function clip(text, max) {
  const clean = text.replace(/\s+/g, ' ').trim();
  if (clean.length <= max) return clean;
  const cut = clean.slice(0, max - 1);
  return `${cut.slice(0, Math.max(cut.lastIndexOf(' '), max / 2))}…`;
}

async function downloadImage(url, name) {
  const file = join(CACHE, name);
  if (!existsSync(file)) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`${url} answered ${response.status}`);
    writeFileSync(file, Buffer.from(await response.arrayBuffer()));
  }
  const type = file.endsWith('.png') ? 'image/png' : file.endsWith('.jpg') ? 'image/jpeg' : 'image/webp';
  return new File([readFileSync(file)], name, { type });
}

// ---------------------------------------------------------------------------------------------------- the seeding

let token;
const sdk = createEcomSdk({ tokenStore: () => ({ getAccessToken: () => token, setAccessToken: (t) => (token = t) }) });
const api = sdk.with();

async function retry(what, fn, attempts = 20) {
  for (let attempt = 1; ; attempt++) {
    try {
      return await fn();
    } catch (error) {
      if (attempt >= attempts) throw new Error(`${what}: ${error.message ?? error}`, { cause: error });
      await sleep(1000);
    }
  }
}

async function seedProduct(source, index) {
  const extension = (url) => (/\.(png|jpe?g|webp)(\?|$)/i.exec(url)?.[1] ?? 'webp').toLowerCase().replace('jpeg', 'jpg');
  const files = [];
  for (const [n, url] of source.images.slice(0, IMAGES_PER_PRODUCT).entries()) {
    files.push(await retry(`image ${url}`, () => downloadImage(url, `${source.id}-${n}.${extension(url)}`), 3));
  }
  const uploaded = await api.assets.uploadImages({ files });
  const images = uploaded.items.map((item) => item.url);

  const basePrice = Math.max(1, Math.round(source.price * USD_TO_INR));
  const product = await api.products.createProduct({
    title: clip(source.title, 50).padEnd(5, '.'),
    description: clip(source.description, 200),
    defaultPrice: basePrice,
    images,
  });

  const skus = [];
  for (const variant of variantsFor(source.category, SKUS_PER_PRODUCT)) {
    const sku = await api.products.createSku({
      productId: product.id,
      price: Math.max(1, Math.round(basePrice * variant.factor)),
      properties: { [variant.name]: variant.value },
    });
    skus.push(sku);
  }

  // the inventory service creates a SKU's stock record from an event, so it may take a moment to exist
  for (const [n, sku] of skus.entries()) {
    const outOfStock = (index + n) % 17 === 0;
    const totalQuantity = outOfStock ? 0 : 5 + ((index * 7 + n * 11) % 56);
    await retry(`stock for SKU ${sku.skuId}`, () => api.inventory.adjustStock({ skuId: sku.skuId, totalQuantity }));
  }
  return { product, skus: skus.length };
}

async function main() {
  const started = Date.now();
  await reset();

  log('Creating the seller');
  const session = await api.auth.registerSeller(SELLER);
  log(`  ${SELLER.email} (${session.user.id})`);

  log('Loading products');
  const sources = pick(await loadCatalog(), PRODUCTS);
  if (sources.length < PRODUCTS) throw new Error(`Only ${sources.length} products available`);

  log(`Creating ${sources.length} products with ${SKUS_PER_PRODUCT} SKUs each`);
  let done = 0;
  let skuCount = 0;
  const queue = sources.map((source, index) => ({ source, index }));
  await Promise.all(
    Array.from({ length: CONCURRENCY }, async () => {
      for (let job = queue.shift(); job; job = queue.shift()) {
        const result = await seedProduct(job.source, job.index);
        skuCount += result.skus;
        log(`  ${String(++done).padStart(2)}/${sources.length} ${result.product.title}`);
      }
    }),
  );

  log('Waiting for the catalog');
  const catalog = await retry(
    'catalog',
    async () => {
      const page = await api.catalog.listCatalogProducts({ size: 1 });
      if (page.totalElements < sources.length) throw new Error(`${page.totalElements}/${sources.length} listed`);
      return page;
    },
    60,
  );
  const available = (await api.catalog.listCatalogProducts({ size: 1, availability: 'AVAILABLE' })).totalElements;

  log(`Done in ${Math.round((Date.now() - started) / 1000)}s: ${catalog.totalElements} products, ${skuCount} SKUs, ${available} in stock.`);
  log(`Seller sign-in: ${SELLER.email} / ${SELLER.password}`);
}

main().catch((error) => {
  console.error('\nIngestion failed:', error.message ?? error);
  process.exit(1);
});
