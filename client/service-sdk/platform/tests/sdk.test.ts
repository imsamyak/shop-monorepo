import { describe, expect, it } from 'vitest';
import {
  addCartItem,
  createEcomSdk,
  ecomSdkBuilder,
  getCart,
  getInventories,
  SdkAuthError,
  SdkBuilder,
  SdkHttpError,
  SdkNetworkError,
  SdkValidationError,
  useCase,
} from '../src/index.js';
import { CUSTOMER, type Ctx, ctxTokenStore, fakeFetch } from './helpers.js';

const unexpected = (): never => {
  throw new Error('expected the call to fail');
};

const BASE_URLS = {
  identity: 'http://identity',
  checkout: 'http://checkout/',
  inventory: 'http://inventory',
};

function sdkWith(fake: ReturnType<typeof fakeFetch>, validation?: 'strict' | 'shape' | 'off') {
  return createEcomSdk<Ctx>({ tokenStore: ctxTokenStore, baseUrls: BASE_URLS, fetch: fake.fetch, validation });
}

describe('request building', () => {
  it('sorts one flat request into path params and body', async () => {
    const cart = { customerId: CUSTOMER, items: [{ skuId: 7, quantity: 2 }], version: 1, updatedAt: 'now' };
    const fake = fakeFetch(200, cart);

    const result = await sdkWith(fake).with({ token: 't' }).cart.addCartItem({ customerId: CUSTOMER, skuId: 7, quantity: 2 });

    expect(result).toEqual(cart);
    expect(fake.requests).toEqual([
      {
        url: `http://checkout/carts/${CUSTOMER}/items`,
        method: 'POST',
        headers: { accept: 'application/json', authorization: 'Bearer t', 'content-type': 'application/json' },
        body: { skuId: 7, quantity: 2 },
      },
    ]);
  });

  it('puts non-path fields in the query on GET and DELETE', async () => {
    const fake = fakeFetch(200, { items: [] });
    const { inventory, cart, products } = sdkWith(fake).with({ token: 't' });

    await inventory.getInventories({ skuIds: [1, 2] });
    await cart.removeCartItem({ customerId: CUSTOMER, skuId: 3, version: 4 });
    await products.listProducts();

    expect(fake.requests.map((r) => [r.method, r.url, r.body])).toEqual([
      ['GET', 'http://inventory/inventory?skuIds=1&skuIds=2', undefined],
      ['DELETE', `http://checkout/carts/${CUSTOMER}/items/3?version=4`, undefined],
      ['GET', 'http://localhost:8081/products', undefined],
    ]);
  });

  it('sends an empty JSON body when every body field is optional', async () => {
    const fake = fakeFetch(201, {});
    await sdkWith(fake).with({ token: 't' }).products.createSku({ productId: CUSTOMER });
    expect(fake.requests[0]?.body).toEqual({});
  });

  it('returns undefined for an empty response', async () => {
    const fake = fakeFetch(202);
    await expect(sdkWith(fake).with({}).auth.forgotPassword({ email: 'a@b.c' })).resolves.toBeUndefined();
  });

  it('reaches the same use cases through execute', async () => {
    const fake = fakeFetch(200, {});
    await sdkWith(fake).execute('GetCart', { customerId: CUSTOMER }, { token: 't' });
    expect(fake.requests[0]?.url).toBe(`http://checkout/carts/${CUSTOMER}`);
  });
});

describe('images', () => {
  it('returns the images the backend sends, in order', async () => {
    const page = { items: [{ id: 'p1', images: ['https://f/2.png', 'https://f/1.png'], skus: [] }], page: 0, size: 20, totalElements: 1, totalPages: 1 };

    const result = await sdkWith(fakeFetch(200, page)).with({}).catalog.listCatalogProducts();

    expect(result.items[0]?.images).toEqual(['https://f/2.png', 'https://f/1.png']);
  });

  it('sends a product with images and no seller', async () => {
    const fake = fakeFetch(201, { id: 'p1', images: ['https://f/1.png'] });

    await sdkWith(fake)
      .with({ token: 't' })
      .products.createProduct({ title: 'Laptop Pro', description: 'd', defaultPrice: 10, images: ['https://f/1.png'] });

    expect(fake.requests[0]?.body).toEqual({ title: 'Laptop Pro', description: 'd', defaultPrice: 10, images: ['https://f/1.png'] });
  });
});

describe('uploading images', () => {
  const png = () => new Blob([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], { type: 'image/png' });

  it('sends the files as multipart form data, in order, with the token', async () => {
    let seen: RequestInit | undefined;
    const fetch = (async (_url: string | URL | Request, init?: RequestInit) => {
      seen = init;
      return new Response(JSON.stringify({ items: [] }), { status: 201 });
    }) as unknown as typeof globalThis.fetch;
    const sdk = createEcomSdk<Ctx>({ tokenStore: ctxTokenStore, baseUrls: { asset: 'http://asset' }, fetch });

    await sdk.with({ token: 't' }).assets.uploadImages({ files: [png(), png()] });

    expect(seen?.body).toBeInstanceOf(FormData);
    expect((seen?.body as FormData).getAll('files')).toHaveLength(2);
    const headers = seen?.headers as Record<string, string>;
    expect(headers['content-type']).toBeUndefined();
    expect(headers.authorization).toBe('Bearer t');
  });

  it('refuses no files and more than ten before calling the backend', async () => {
    const fake = fakeFetch(201, { items: [] });
    const assets = sdkWith(fake, 'strict').with({ token: 't' }).assets;

    await expect(assets.uploadImages({ files: [] })).rejects.toBeInstanceOf(SdkValidationError);
    await expect(assets.uploadImages({ files: Array.from({ length: 11 }, png) })).rejects.toBeInstanceOf(SdkValidationError);
    expect(fake.requests).toHaveLength(0);
  });
});

describe('seller sign-up', () => {
  it('stores the token of a new seller', async () => {
    const ctx: Ctx = {};
    const fake = fakeFetch(201, { accessToken: 'seller-token', user: { role: 'ROLE_SELLER' } });

    await sdkWith(fake).with(ctx).auth.registerSeller({ email: 'a@b.co', password: 'Passw0rd!x' });

    expect(fake.requests[0]?.url).toBe('http://identity/auth/register/seller');
    expect(ctx.token).toBe('seller-token');
  });
});

describe('access token', () => {
  it('stores the token from login in the store of that call context', async () => {
    const fake = fakeFetch(200, { accessToken: 'tok-1', tokenType: 'Bearer', expiresIn: 900, user: {} });
    const alice: Ctx = {};
    const bob: Ctx = {};

    await sdkWith(fake).with(alice).auth.login({ email: 'a@b.c', password: 'secret' });

    expect(alice.token).toBe('tok-1');
    expect(bob.token).toBeUndefined();
  });

  it('sends the stored token only to use cases that require it', async () => {
    const fake = fakeFetch(200, {});
    const { auth, products } = sdkWith(fake).with({ token: 'tok-1' });

    await auth.getCurrentUser();
    await products.listProducts();

    expect(fake.requests[0]?.headers.authorization).toBe('Bearer tok-1');
    expect(fake.requests[1]?.headers.authorization).toBeUndefined();
  });

  it('fails without sending when a required token is missing', async () => {
    const fake = fakeFetch(200, {});
    await expect(sdkWith(fake).with({}).auth.getCurrentUser()).rejects.toBeInstanceOf(SdkAuthError);
    expect(fake.requests).toHaveLength(0);
  });

  it('clears the token on logout, even without sending it', async () => {
    const fake = fakeFetch(204);
    const ctx: Ctx = { token: 'tok-1' };

    await sdkWith(fake).with(ctx).auth.logout();

    expect(ctx.token).toBeUndefined();
    expect(fake.requests[0]?.headers.authorization).toBeUndefined();
  });

  it('does not touch the store when login fails', async () => {
    const fake = fakeFetch(401, { status: 401, errorCode: 'UNAUTHORIZED' });
    const ctx: Ctx = { token: 'old' };
    await expect(sdkWith(fake).with(ctx).auth.login({ email: 'a', password: 'b' })).rejects.toThrow();
    expect(ctx.token).toBe('old');
  });
});

describe('validation', () => {
  it('rejects bad path params in every mode, before sending', async () => {
    const fake = fakeFetch(200, {});
    const error = await sdkWith(fake, 'off')
      .with({ token: 't' })
      .cart.getCart({ customerId: 'not-a-uuid' })
      .then(unexpected, (e: unknown) => e as SdkValidationError);

    expect(error).toBeInstanceOf(SdkValidationError);
    expect(error.part).toBe('params');
    expect(Object.keys(error.invalidFields)).toEqual(['customerId']);
    expect(fake.requests).toHaveLength(0);
  });

  it('names nested fields the way the backend does', async () => {
    const error = await sdkWith(fakeFetch(), 'strict')
      .with({})
      .checkout.draftCheckout({ items: [{ skuId: 1, quantity: 5000 }] })
      .then(unexpected, (e: unknown) => e as SdkValidationError);
    expect(Object.keys(error.invalidFields)).toEqual(['items[0].quantity']);
  });

  it('leaves body limits to the backend in shape mode but still checks types and unknown keys', async () => {
    const big = { customerId: CUSTOMER, skuId: 1, quantity: 5000 };

    await expect(sdkWith(fakeFetch(200, {}), 'shape').with({ token: 't' }).cart.addCartItem(big)).resolves.toEqual({});
    await expect(sdkWith(fakeFetch(200, {}), 'strict').with({ token: 't' }).cart.addCartItem(big)).rejects.toBeInstanceOf(
      SdkValidationError,
    );

    const typo = { customerId: CUSTOMER, skuId: 1, quantity: 1, quantiy: 2 };
    await expect(sdkWith(fakeFetch(200, {}), 'shape').with({ token: 't' }).cart.addCartItem(typo)).rejects.toBeInstanceOf(
      SdkValidationError,
    );
  });

  it('rejects fields a use case does not take', async () => {
    const error = await sdkWith(fakeFetch(200, {}), 'off')
      .execute('CommitCheckout', { checkoutId: CUSTOMER, force: true } as never, {})
      .then(unexpected, (e: unknown) => e as SdkValidationError);
    expect(error.invalidFields).toEqual({ force: 'Unrecognized key' });
  });

  it('keeps bounds of 0 and 1 in shape mode', async () => {
    const zero = { customerId: CUSTOMER, skuId: 1, quantity: 0 };
    await expect(sdkWith(fakeFetch(200, {}), 'shape').with({ token: 't' }).cart.addCartItem(zero)).rejects.toBeInstanceOf(
      SdkValidationError,
    );
  });

  it('sends the body as given when validation is off', async () => {
    const fake = fakeFetch(200, {});
    await sdkWith(fake, 'off').with({ token: 't' }).inventory.addStock({ skuId: 1, quantity: -5 });
    expect(fake.requests[0]?.body).toEqual({ quantity: -5 });
  });
});

describe('errors', () => {
  it('wraps a non-2xx answer with the backend error body', async () => {
    const body = { status: 409, errorCode: 'VERSION_CONFLICT', detail: 'stale' };
    const error = await sdkWith(fakeFetch(409, body))
      .with({ token: 't' })
      .cart.getCart({ customerId: CUSTOMER })
      .then(unexpected, (e: unknown) => e as SdkHttpError);

    expect(error).toBeInstanceOf(SdkHttpError);
    expect(error.status).toBe(409);
    expect(error.errorCode).toBe('VERSION_CONFLICT');
    expect(error.useCase).toBe('GetCart');
  });

  it('wraps a failed fetch', async () => {
    const sdk = createEcomSdk<Ctx>({
      tokenStore: ctxTokenStore,
      fetch: async () => {
        throw new TypeError('connection refused');
      },
    });
    await expect(sdk.with({ token: 't' }).orders.getOrder({ orderId: CUSTOMER })).rejects.toBeInstanceOf(SdkNetworkError);
  });
});

describe('builder', () => {
  const tokenStore = () => ctxTokenStore({});

  it('builds an SDK with only the use cases added', () => {
    const sdk = SdkBuilder.create({ tokenStore, baseUrls: {} }).addUseCase(getCart).addUseCase(addCartItem).build();
    expect(sdk.names()).toEqual(['GetCart', 'AddCartItem']);
    expect(sdk.has('GetOrder')).toBe(false);
  });

  it('groups use cases into gateways, one method per use case', () => {
    const sdk = SdkBuilder.create({ tokenStore, baseUrls: {} })
      .addGateway('basket', [getCart, addCartItem])
      .addGateway('stock', [getInventories])
      .build();
    const gateways = sdk.with();
    expect(Object.keys(gateways)).toEqual(['basket', 'stock']);
    expect(Object.keys(gateways.basket)).toEqual(['getCart', 'addCartItem']);
  });

  it('rejects the same name twice', () => {
    expect(() => SdkBuilder.create({ tokenStore, baseUrls: {} }).addUseCase(getCart).addUseCase(getCart)).toThrow(/already added/);
    expect(() => SdkBuilder.create({ tokenStore, baseUrls: {} }).addGateway('a', []).addGateway('a', [])).toThrow(/already added/);
    const impostor = useCase('GetCart').service('order').get('/x').build();
    expect(() => SdkBuilder.create({ tokenStore, baseUrls: {} }).addGateway('a', [getCart]).addGateway('b', [impostor])).toThrow(
      /Another use case/,
    );
  });

  it('refuses a use case without a service or route', () => {
    expect(() => useCase('NoService').get('/x').build()).toThrow(/no service/);
    expect(() => useCase('NoRoute').service('order').build()).toThrow(/no method and path/);
  });

  it('adds a custom gateway next to the built-in ones', async () => {
    const ping = useCase('Ping').service('order').get('/actuator/health').returns<{ status: string }>().build();
    const fake = fakeFetch(200, { status: 'UP' });
    const sdk = ecomSdkBuilder({ tokenStore, fetch: fake.fetch, baseUrls: { order: 'http://order' } })
      .addGateway('health', [ping])
      .build();
    await expect(sdk.with().health.ping()).resolves.toEqual({ status: 'UP' });
    expect(fake.requests[0]?.url).toBe('http://order/actuator/health');
  });

  it('does not change a builder that was extended', () => {
    const base = SdkBuilder.create({ tokenStore, baseUrls: {} }).addUseCase(getCart);
    base.addUseCase(getInventories);
    expect(base.build().names()).toEqual(['GetCart']);
  });
});
