# shop-monorepo

```
server/     everything that runs on a server
  platform/       the backend: Spring Boot services and modules, built with Maven (docs/api.json lists the endpoints)
  chatbot/        the shopping assistant (Node, Express)           (port 4001)
client/     everything customers and sellers use, and the code that talks to the server
  service-sdk/    one typed client per server service
    platform/       @shop/platform-sdk, the client of server/platform
    chatbot/        @shop/chatbot-sdk, the client of server/chatbot
  packages/       code shared by the apps and SDKs (sdk-core, ui, config)
  apps/           the web apps; cartly is the first (Next.js)
```

The TypeScript code (`client/` and the Node services in `server/`) is one pnpm workspace rooted here: run `pnpm install`
once and the packages find each other by name (`"@shop/platform-sdk": "workspace:*"`). `server/platform` is a separate
Maven build (`cd server/platform && mvn clean install`).
