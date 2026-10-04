import path from 'node:path';
import type { NextConfig } from 'next';

// The app lives three folders below the workspace root and uses packages from outside its own folder.
const workspaceRoot = path.join(__dirname, '../../..');

const nextConfig: NextConfig = {
  // a self-contained server for the Docker image (client/apps/cartly/Dockerfile)
  output: 'standalone',
  // The shared packages are TypeScript source, so Next compiles them with the app.
  transpilePackages: ['@cartly/chat'],
  turbopack: { root: workspaceRoot },
  outputFileTracingRoot: workspaceRoot,
  // A seller uploads up to ten pictures of 5 MB through a server action.
  experimental: { serverActions: { bodySizeLimit: '60mb' } },
};

export default nextConfig;
