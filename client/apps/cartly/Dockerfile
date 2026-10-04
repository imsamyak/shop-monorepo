# syntax=docker/dockerfile:1
# Cartly, the storefront. Build from the repository root (it uses the workspace's SDKs and packages):
#   docker build -f client/apps/cartly/Dockerfile -t shop-cartly .
# Run it with ECOM_API_URL (or the ECOM_<SERVICE>_URL variables) and CHATBOT_URL pointing at the servers.

FROM node:24-slim AS build
RUN corepack enable
WORKDIR /repo

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY client ./client
RUN pnpm install --frozen-lockfile --filter "@cartly/web..."

# `build` first builds the SDKs this app imports (prebuild), then the standalone Next server
RUN pnpm --filter @cartly/web build

FROM node:24-slim
WORKDIR /app
ENV NODE_ENV=production PORT=3000 HOSTNAME=0.0.0.0
# the standalone output keeps the monorepo's folder layout, so the server is at client/apps/cartly/server.js
COPY --from=build /repo/client/apps/cartly/.next/standalone ./
COPY --from=build /repo/client/apps/cartly/.next/static ./client/apps/cartly/.next/static
COPY --from=build /repo/client/apps/cartly/public ./client/apps/cartly/public

EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://localhost:3000/health').then((r) => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
USER node
CMD ["node", "client/apps/cartly/server.js"]
