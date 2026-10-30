import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export const devDir = join(dirname(fileURLToPath(import.meta.url)), '../..');
export const repoRoot = join(devDir, '../..');

const defaults = {
  user: 'ec2-user',
  // the folder on the server (relative to its home) that the code is copied to
  dir: 'shop-monorepo',
  // true: add the Cloudflare Tunnel (https), and mark the login cookie Secure
  tunnel: false,
};

/** The server's address and key, from bootstrap/dev/deploy.config.json (copy deploy.config.example.json). */
export function loadConfig() {
  const file = join(devDir, 'deploy.config.json');
  if (!existsSync(file)) throw new Error(`Missing ${file}: copy deploy.config.example.json and fill in the host and key.`);
  const config = { ...defaults, ...JSON.parse(readFileSync(file, 'utf8')) };
  if (!config.host) throw new Error('deploy.config.json needs "host"');
  // a relative key path is relative to bootstrap/dev (where the key is kept, git-ignored)
  config.keyPath = config.keyPath && resolve(devDir, config.keyPath);
  if (!config.keyPath || !existsSync(config.keyPath)) throw new Error(`deploy.config.json "keyPath" is not a file: ${config.keyPath}`);
  return config;
}

/** The settings and secrets to put on the server: bootstrap/dev/.env, or else the repository's own .env (same variables). */
export function envFile() {
  const found = [join(devDir, '.env'), join(repoRoot, '.env')].find(existsSync);
  if (!found) throw new Error('No .env to upload: create bootstrap/dev/.env from .env.example (or have one in the repository root).');
  return found;
}
