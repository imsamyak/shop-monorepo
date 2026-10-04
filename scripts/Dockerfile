# syntax=docker/dockerfile:1
# Seeds a new environment once: sellers and products through the platform's API. Build from the repository root:
#   docker build -f scripts/Dockerfile -t shop-ingest .
# Point it at the services with ECOM_<SERVICE>_URL. It does not reset anything; `--seed-only` is what skips that.

FROM node:24-slim
RUN corepack enable
WORKDIR /repo

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY client/packages/sdk-core/package.json client/packages/sdk-core/
COPY client/service-sdk/platform/package.json client/service-sdk/platform/
COPY scripts/package.json scripts/
RUN pnpm install --frozen-lockfile --filter "@shop/scripts..."

COPY client/packages/sdk-core client/packages/sdk-core
COPY client/service-sdk/platform client/service-sdk/platform
COPY scripts scripts
RUN pnpm --filter @shop/sdk-core build && pnpm --filter @shop/platform-sdk build

WORKDIR /repo/scripts
USER node
CMD ["node", "src/ingest/index.mjs", "--seed-only"]
