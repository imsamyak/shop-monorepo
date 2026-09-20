# @shop/sdk-core

The engine the typed service SDKs (`client/service-sdk/*`) are built on: the `useCase` builder, the `SdkBuilder` and its
gateways, token handling, request validation and the error classes. It knows nothing about any particular service; an SDK
supplies its use cases and the base URL of each service name they use.

```ts
const sdk = SdkBuilder.create({ tokenStore, baseUrls: { chatbot: 'http://localhost:4001' } })
  .addGateway('chat', chatUseCases)
  .build();
await sdk.with(ctx).chat.chat({ message: 'hi', sessionId });
```

The engine is exercised through the SDKs' own tests (`client/service-sdk/platform/tests`).
