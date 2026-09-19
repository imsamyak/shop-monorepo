// Run with `pnpm test`. A stub checkout service stands in for the platform, so no database or Gemini key is needed.

import assert from "node:assert/strict";
import http from "node:http";
import { after, before, describe, it } from "node:test";

const seen = [];
const stub = http.createServer((req, res) => {
  let body = "";
  req.on("data", (c) => (body += c));
  req.on("end", () => {
    seen.push({ method: req.method, url: req.url, authorization: req.headers.authorization, body });
    res.setHeader("content-type", "application/json");
    res.end(JSON.stringify({ customerId: USER, items: [{ skuId: 7, quantity: 2 }], version: 1, updatedAt: "2026-01-01T00:00:00Z" }));
  });
});

const USER = "3f0c8a52-7a39-4d4f-9a1c-0d6f3a9d2b11";
const token = `x.${Buffer.from(JSON.stringify({ sub: USER })).toString("base64url")}.y`;

let tools;
let auth;
before(async () => {
  await new Promise((resolve) => stub.listen(0, resolve));
  process.env.ECOM_CHECKOUT_URL = `http://localhost:${stub.address().port}`;
  auth = await import("../src/auth.js");
  tools = await import("../src/agent/tools.js");
});
after(() => stub.close());

describe("auth", () => {
  it("reads the bearer token and the customer id in it", () => {
    const req = { get: () => `Bearer ${token}` };
    assert.equal(auth.bearerToken(req), token);
    assert.equal(auth.userIdOf(token), USER);
  });

  it("rejects missing and malformed tokens", () => {
    assert.equal(auth.bearerToken({ get: () => "" }), null);
    assert.equal(auth.bearerToken({ get: () => "Basic abc" }), null);
    assert.equal(auth.userIdOf("not-a-jwt"), null);
  });
});

describe("add_to_cart", () => {
  const run = (input, ctx) => tools.runTool("add_to_cart", input, ctx);

  it("adds nothing until the customer has confirmed in a later message", async () => {
    seen.length = 0;
    const ctx = { token, userId: USER, session: { turn: 1, pending: null } };

    const first = await run({ skuId: 7, quantity: 2 }, ctx);
    assert.equal(first.status, "needs_confirmation");
    assert.equal(seen.length, 0);

    // same turn again: the model confirming on its own does not count
    const sameTurn = await run({ skuId: 7, quantity: 2 }, ctx);
    assert.equal(sameTurn.status, "needs_confirmation");
    assert.equal(seen.length, 0);

    ctx.session.turn = 2; // the customer answered
    const added = await run({ skuId: 7, quantity: 2 }, ctx);
    assert.equal(added.items[0].skuId, 7);
    assert.equal(seen.length, 1);
    assert.equal(seen[0].method, "POST");
    assert.equal(seen[0].url, `/carts/${USER}/items`);
    assert.equal(seen[0].authorization, `Bearer ${token}`);
    assert.equal(ctx.session.pending, null);
  });

  it("asks again when the customer changed the request", async () => {
    seen.length = 0;
    const ctx = { token, userId: USER, session: { turn: 1, pending: null } };
    await run({ skuId: 7, quantity: 2 }, ctx);
    ctx.session.turn = 2;
    const changed = await run({ skuId: 7, quantity: 5 }, ctx);
    assert.equal(changed.status, "needs_confirmation");
    assert.equal(seen.length, 0);
  });
});

describe("get_cart", () => {
  it("reads the signed-in customer's cart with their token", async () => {
    seen.length = 0;
    await tools.runTool("get_cart", {}, { token, userId: USER });
    assert.equal(seen[0].url, `/carts/${USER}`);
    assert.equal(seen[0].authorization, `Bearer ${token}`);
  });
});
