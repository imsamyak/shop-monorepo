# Shop Chatbot (RAG + agent)

An AI shopping assistant for the shop platform, built with Node.js, Express and Google Gemini. It can

- answer policy questions (shipping, returns, payments) from the documents in `knowledge/` (**RAG**)
- look up live products, the customer's cart and orders through `@shop/platform-sdk` (**agent with tools**)
- add an item to the cart, but only after the customer has confirmed it in a later message
- remember the conversation ("add 2 of *that* one")

It cannot check out or place orders.

## How it fits the platform

```
app (Cartly)  --POST /api/chat, Bearer token-->  chatbot (4001)  --@shop/platform-sdk, same token-->  platform services
```

- The customer is whoever the token says. The chatbot passes that token on to the services, which verify it and enforce
  ownership; the request body never names a customer.
- Conversations are kept in memory, keyed by customer and `sessionId`, and dropped after an hour idle.
- Titles and descriptions come from sellers, so tool results label them as data and the model is told never to follow
  instructions found in them.
- `docs/api.json` lists the endpoint; `client/service-sdk/chatbot` is its typed client.

## Run it

```bash
cp .env.example .env        # then set GEMINI_API_KEY
pnpm ingest                 # builds data/index.json from knowledge/ (first run downloads a ~25 MB embedding model)
pnpm dev                    # http://localhost:4001
```

The platform services have to be running for the shop tools; point the SDK at them with `ECOM_API_URL` or the
`ECOM_<SERVICE>_URL` variables (defaults are the local ports). In development `/` serves a small test page where you paste
an access token. `pnpm test` runs the tests against a stub service (no Gemini key needed).

## Files

| Path | What it is |
| --- | --- |
| `src/server.js` | Express app: token check, per-customer sessions, `POST /api/chat`, `GET /health` |
| `src/agent/agent.js` | the agent loop with Gemini |
| `src/agent/tools.js` | the tools the model may call |
| `src/shopApi.js` | the platform calls, through the SDK |
| `src/rag/` | embeddings, ingest and the vector store |
| `knowledge/` | the help documents the bot answers policy questions from |
