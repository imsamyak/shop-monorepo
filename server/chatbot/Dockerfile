# syntax=docker/dockerfile:1
# The shopping assistant. Build from the repository root (it uses the workspace's platform SDK):
#   docker build -f server/chatbot/Dockerfile -t shop-chatbot .
# Run it with GEMINI_API_KEY and, where the platform is not on localhost, ECOM_API_URL or the ECOM_<SERVICE>_URL variables.
# The image is Debian based because the embedding model's runtime (onnxruntime) does not run on Alpine.

FROM node:24-slim
RUN corepack enable
WORKDIR /repo

# dependencies first, so a code change does not install them again
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY client/packages/sdk-core/package.json client/packages/sdk-core/
COPY client/service-sdk/platform/package.json client/service-sdk/platform/
COPY server/chatbot/package.json server/chatbot/
RUN pnpm install --frozen-lockfile --filter "@shop/chatbot..."

COPY client/packages/sdk-core client/packages/sdk-core
COPY client/service-sdk/platform client/service-sdk/platform
COPY server/chatbot server/chatbot
RUN pnpm --filter @shop/sdk-core build && pnpm --filter @shop/platform-sdk build

# the knowledge index is built here, so the container starts without downloading the embedding model
WORKDIR /repo/server/chatbot
RUN pnpm ingest

ENV NODE_ENV=production PORT=4001
EXPOSE 4001
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://localhost:4001/health').then((r) => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"
USER node
CMD ["node", "src/server.js"]
