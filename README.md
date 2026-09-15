# shop-monorepo

| Folder | What it is |
|---|---|
| `platform/` | The backend: Spring Boot services and modules, built with Maven. `platform/docs/api.json` lists the endpoints. |
| `platform-sdk/` | `@shop/platform-sdk`, the typed TypeScript client for the platform. |
| `storefront/` | Everything customers and sellers see: the web apps (`client/`) and the small Node services behind them (`server/`). |

The TypeScript code (`platform-sdk` and everything in `storefront`) is one pnpm workspace: run `pnpm install` once at this
folder and the packages find each other by name (`"@shop/platform-sdk": "workspace:*"`). The platform is a separate
Maven build (`cd platform && mvn clean install`).
