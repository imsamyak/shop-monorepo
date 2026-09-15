# storefront

```
client/    web apps (React / Next.js) and the packages they share
server/    small Express services and the packages they share
```

Both sides use the platform only through `@shop/platform-sdk`; nothing here calls a platform URL by hand.
Code is shared through `packages/`, never by one app importing another.
