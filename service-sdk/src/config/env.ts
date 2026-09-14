import type { ServiceName } from '../core/types.js';

export type BaseUrls = Record<ServiceName, string>;

type Env = Record<string, string | undefined>;

/** Per-service variable; it wins over `ECOM_API_URL`. */
export const SERVICE_URL_ENV: Readonly<Record<ServiceName, string>> = {
  identity: 'ECOM_IDENTITY_URL',
  catalog: 'ECOM_CATALOG_URL',
  listing: 'ECOM_LISTING_URL',
  checkout: 'ECOM_CHECKOUT_URL',
  order: 'ECOM_ORDER_URL',
  inventory: 'ECOM_INVENTORY_URL',
  asset: 'ECOM_ASSET_URL',
};

/** One base URL for every service, for when they sit behind a gateway. */
export const API_URL_ENV = 'ECOM_API_URL';

/** The `server.port` defaults of the services' `application.yml`. */
const LOCAL_DEFAULTS: Readonly<BaseUrls> = {
  identity: 'http://localhost:8086',
  catalog: 'http://localhost:8087',
  listing: 'http://localhost:8081',
  checkout: 'http://localhost:8084',
  order: 'http://localhost:8085',
  inventory: 'http://localhost:8083',
  asset: 'http://localhost:8088',
};

function processEnv(): Env {
  return (globalThis as { process?: { env?: Env } }).process?.env ?? {};
}

const trimSlash = (url: string): string => url.replace(/\/+$/, '');

/** Base URL of every service: the per-service variable, else `ECOM_API_URL`, else the local default. */
export function resolveBaseUrls(env: Env = processEnv(), overrides: Partial<BaseUrls> = {}): BaseUrls {
  const shared = env[API_URL_ENV];
  const urls = {} as BaseUrls;
  for (const service of Object.keys(LOCAL_DEFAULTS) as ServiceName[]) {
    urls[service] = trimSlash(overrides[service] ?? env[SERVICE_URL_ENV[service]] ?? shared ?? LOCAL_DEFAULTS[service]);
  }
  return urls;
}
