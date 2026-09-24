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
```

The TypeScript code (`client/` and the Node services in `server/`) is one pnpm workspace rooted here: run `pnpm install`
once and the packages find each other by name (`"@shop/platform-sdk": "workspace:*"`). `server/platform` is a separate
Maven build (`cd server/platform && mvn clean install`).

## Run everything for development

Start the resources once (`server/platform/bootstrap/local/local.ps1 up`), then:

```
server/platform/bootstrap/local/dev-service.ps1 <identity|inventory|order|catalog|listing|checkout|asset>
./dev-rest.ps1        # checkout, asset, chatbot and Cartly in one window
```

Each Java service restarts itself when its code, `module/*` or `infra` changes (a shared module is installed first).
The chatbot restarts with nodemon and Cartly hot-reloads. Start inventory, order and catalog before listing and checkout,
because a RabbitMQ message sent before its consumer's queue exists is dropped.

