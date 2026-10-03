// Creates sellers, their products, SKUs and stock through the platform's own API, as the seller.

import { createEcomSdk } from '@shop/platform-sdk';
import { picture } from './images.mjs';

const CONCURRENCY = 3;

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

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

/** The platform's API as one seller: registering them signs them in. */
async function registerSeller({ email, password }) {
  let token;
  const sdk = createEcomSdk({ tokenStore: () => ({ getAccessToken: () => token, setAccessToken: (t) => (token = t) }) });
  const api = sdk.with();
  const session = await api.auth.registerSeller({ email, password });
  return { api, id: session.user.id };
}

async function seedProduct(api, source) {
  const files = [];
  for (const path of source.images) files.push(picture(path));
  const uploaded = await api.assets.uploadImages({ files });

  const product = await api.products.createProduct({
    title: source.title,
    description: source.description,
    defaultPrice: source.price,
    images: uploaded.items.map((item) => item.url),
  });

  const skus = [];
  for (const sku of source.skus) {
    skus.push({ created: await api.products.createSku({ productId: product.id, price: sku.price, properties: sku.properties }), stock: sku.stock });
  }

  // the inventory service creates a SKU's stock record from an event, so it may take a moment to exist
  for (const { created, stock } of skus) {
    await retry(`stock for SKU ${created.skuId}`, () => api.inventory.adjustStock({ skuId: created.skuId, totalQuantity: stock }));
  }
  return { title: product.title, skus: skus.length };
}

/** Registers every seller and creates the products that name them. Returns the number of products and SKUs created. */
export async function seed(sellers, products, log) {
  let productCount = 0;
  let skuCount = 0;

  for (const seller of sellers) {
    const { api, id } = await registerSeller(seller);
    log(`  seller ${seller.email} (${id})`);

    const queue = products.filter((product) => product.seller === seller.email);
    const total = queue.length;
    let done = 0;
    await Promise.all(
      Array.from({ length: CONCURRENCY }, async () => {
        for (let source = queue.shift(); source; source = queue.shift()) {
          const result = await seedProduct(api, source);
          productCount += 1;
          skuCount += result.skus;
          log(`    ${String(++done).padStart(2)}/${total} ${result.title}`);
        }
      }),
    );
  }
  return { products: productCount, skus: skuCount };
}
