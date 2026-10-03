// Empties the development shop: sellers and everything built on their listings, carts, checkouts, orders, the catalog and
// the images in S3. Buyer accounts stay.

import { DeleteObjectsCommand, ListObjectsV2Command, S3Client } from '@aws-sdk/client-s3';
import { execFileSync } from 'node:child_process';

const PG_CONTAINER = process.env.SEED_PG_CONTAINER ?? 'ecom-local-postgres-1';
const MONGO_CONTAINER = process.env.SEED_MONGO_CONTAINER ?? 'ecom-local-mongo-1';
const S3_BUCKET = process.env.ASSETS_BUCKET ?? 'imsamyak.shop-asset-dev';
// the asset service keeps everything it uploads under this prefix; nothing else in the bucket is touched
const S3_PREFIX = 'images/';

const LOCAL = /^https?:\/\/(localhost|127\.0\.0\.1)(:|\/|$)/;

/** The services this script talks to must be local; the databases are the local containers by construction. */
function assertLocal() {
  const remote = ['ECOM_API_URL', 'ECOM_IDENTITY_URL', 'ECOM_LISTING_URL', 'ECOM_ASSET_URL', 'ECOM_INVENTORY_URL']
    .map((name) => process.env[name])
    .filter((url) => url && !LOCAL.test(url));
  if (remote.length) throw new Error(`Refusing to reset: the services must be local (${remote.join(', ')})`);
}

function psql(database, sql) {
  return execFileSync('docker', ['exec', PG_CONTAINER, 'psql', '-U', 'ecom', '-d', database, '-v', 'ON_ERROR_STOP=1', '-Atc', sql], {
    encoding: 'utf8',
  }).trim();
}

function mongo(script) {
  return execFileSync('docker', ['exec', MONGO_CONTAINER, 'mongosh', '--quiet', '--eval', script], { encoding: 'utf8' }).trim();
}

async function clearImages() {
  // the bucket's region is written out, and a redirect is followed, so a stray AWS_REGION in the shell cannot break this
  const client = new S3Client({ region: process.env.ASSETS_REGION ?? 'ap-south-1', followRegionRedirects: true });
  let removed = 0;
  for (;;) {
    const listing = await client.send(new ListObjectsV2Command({ Bucket: S3_BUCKET, Prefix: S3_PREFIX, MaxKeys: 1000 }));
    const keys = (listing.Contents ?? []).map((object) => ({ Key: object.Key }));
    if (keys.length === 0) return removed;
    await client.send(new DeleteObjectsCommand({ Bucket: S3_BUCKET, Delete: { Objects: keys, Quiet: true } }));
    removed += keys.length;
  }
}

export async function reset(log) {
  assertLocal();
  log('Resetting development data');

  const sellers = psql('identity_db', "select count(*) from users where role = 'ROLE_SELLER'");
  psql(
    'identity_db',
    "delete from password_reset_tokens where user_id in (select id from users where role = 'ROLE_SELLER'); delete from users where role = 'ROLE_SELLER'",
  );
  log(`  removed ${sellers} seller(s)`);

  psql('listing_db', 'truncate table outbox_events, product_images, product_skus, products restart identity cascade');
  psql('inventory_db', 'truncate table inbox, inventory_items, inventory_reservations, outbox_events restart identity cascade');
  psql('checkout_db', 'truncate table cart_items, carts, checkout_items, checkouts, inbox, outbox_events restart identity cascade');
  psql('order_db', 'truncate table inbox, order_items, orders restart identity cascade');
  log('  emptied listings, stock, carts, checkouts and orders');

  // documents only, so the collections and their indexes stay
  mongo("const d = db.getSiblingDB('catalog'); d.getCollectionNames().forEach((c) => d.getCollection(c).deleteMany({}))");
  log('  emptied the catalog');

  log(`  removed ${await clearImages()} image(s) from s3://${S3_BUCKET}/${S3_PREFIX}`);
}
