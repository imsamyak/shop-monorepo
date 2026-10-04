# shop-monorepo

```
server/     everything that runs on a server
  platform/       the backend: Spring Boot services and modules, built with Maven (docs/api.json lists the endpoints)
  chatbot/        the shopping assistant (Node, Express)           (port 4001)
client/     everything customers and sellers use, and the code that talks to the server
  service-sdk/    one typed client per server service
    platform/       @shop/platform-sdk, the client of server/platform
    chatbot/        @shop/chatbot-sdk, the client of server/chatbot
  packages/       code shared by the apps and SDKs (sdk-core, ui, chat, config)
  apps/           the web apps; cartly is the first (Next.js, with the chat window)
scripts/    development scripts, run by hand (pnpm ingest seeds the local shop)
```

The TypeScript code (`client/` and the Node services in `server/`) is one pnpm workspace rooted here: run `pnpm install`
once and the packages find each other by name (`"@shop/platform-sdk": "workspace:*"`). `server/platform` is a separate
Maven build (`cd server/platform && mvn clean install`).

## Run everything for development

Start the resources once (`server/platform/bootstrap/local/local.ps1 up`), then:

```
server/platform/bootstrap/local/dev-service.ps1 <identity|inventory|order|catalog|listing|checkout|asset>
pnpm dev                 # in server/chatbot, and in client/apps/cartly (one window each)
```

Each Java service restarts itself when its code, `module/*` or `infra` changes (a shared module is installed first).
The chatbot restarts with nodemon and Cartly hot-reloads. Start inventory, order and catalog before listing and checkout,
because a RabbitMQ message sent before its consumer's queue exists is dropped.

## Endpoints

Every endpoint of the platform services and the chatbot is under `/api` (`/api/products`, `/api/auth/login`, `/api/chat`).
Every server also answers `GET /health` (no `/api`, no token) with `{"status":"ok"}`: the platform services get both the
prefix and the probe from one dependency, `server/platform/module/web`; the chatbot and Cartly do it themselves.

## Containers

Each server and app has its own `Dockerfile`, built from the folder named here:

```
(cd server/platform && docker build -f service/<name>/Dockerfile -t shop-<name> .)   # identity, listing, inventory, checkout, order, catalog, asset
docker build -f server/chatbot/Dockerfile -t shop-chatbot .
docker build -f client/apps/cartly/Dockerfile -t shop-cartly .
```

The platform images run the `prod` profile, so their settings arrive as environment variables (see each `application-prod.yml`).

