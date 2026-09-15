# storefront/client

```
apps/        things you run or deploy, one folder each (cartly is the first)
packages/    code the apps share
```

- `apps/` and `packages/` sit side by side. An app imports packages; a package never imports an app, and an app never
  imports another app.
- A new app is a new folder under `apps/` named for what it is (`apps/cartly`, later `apps/seller`, `apps/admin`).
  Its package name is `@cartly/<name>`.
- Next.js apps use ports from 3000 upwards, one each.

Packages: `@cartly/ui` (shared components), `@cartly/config` (shared TypeScript settings).
