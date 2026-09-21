# client

```
service-sdk/    one typed client per server service, named after it (platform/, chatbot/), built on packages/sdk-core
packages/       code the apps share
apps/           things you run or deploy, one folder each (cartly is the first)
```

- An app imports packages and SDKs; a package or an SDK never imports an app, and an app never imports another app.
- Each SDK keeps its service's endpoint list (`server/<service>/docs/api.json`) and a contract test, so the two cannot
  drift apart.
- A new app is a new folder under `apps/` named for what it is. Its package name is `@cartly/<name>`.
- Next.js apps use ports from 3000 upwards, one each.
