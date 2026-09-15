# storefront/server

```
services/    small Express services, one folder each, each its own process and port
packages/    code the services share
```

- A new service is a new folder under `services/` named for what it does. Its package name is `@storefront/<name>`.
- Services use ports from 4001 upwards, one each.
- A service calls the platform only through `@shop/platform-sdk`.
- `services/` and `packages/` sit side by side: a service imports packages, a package never imports a service.

Packages: `@storefront/common` (shared Express pieces).
