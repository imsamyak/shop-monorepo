import type { BaseUrls } from '../config/env.js';
import { type ApiErrorBody, SdkAuthError, SdkHttpError, SdkNetworkError } from '../core/errors.js';
import { fillPath, splitRequest, toQueryString } from '../core/http.js';
import type { TokenStore, TokenStoreProvider } from '../core/token.js';
import type { AnyUseCase, RequestArgs, UseCaseMap, UseCaseRequest, UseCaseResult } from '../core/types.js';
import { type ValidationMode, validatePart } from '../core/validation.js';

export interface SdkOptions<Ctx> {
  /** Called on every call with that call's context. */
  readonly tokenStore: TokenStoreProvider<Ctx>;
  /** Overrides the base URLs read from the environment. */
  readonly baseUrls?: Partial<BaseUrls>;
  /** How much of a body is checked before sending; defaults to `shape`. */
  readonly validation?: ValidationMode;
  /** Defaults to the global `fetch`. */
  readonly fetch?: typeof fetch;
  /** Sent with every request. */
  readonly headers?: Readonly<Record<string, string>>;
}

export interface SdkConfig<Ctx> {
  readonly tokenStore: TokenStoreProvider<Ctx>;
  readonly baseUrls: BaseUrls;
  readonly validation: ValidationMode;
  readonly fetch: typeof fetch;
  readonly headers: Readonly<Record<string, string>>;
}

/** Use cases grouped under a gateway name, as the builder collects them. */
export type GatewayMap = Record<string, readonly AnyUseCase[]>;

/** One method per use case, named after it: `AddCartItem` becomes `addCartItem`. */
export type Gateway<L extends readonly AnyUseCase[]> = {
  readonly [U in L[number] as Uncapitalize<U['name']>]: (...request: RequestArgs<U>) => Promise<UseCaseResult<U>>;
};

export type Gateways<G extends GatewayMap> = { readonly [K in keyof G]: Gateway<G[K]> };

/** The context argument is optional when `Ctx` allows `undefined`. */
export type ContextArgs<Ctx> = undefined extends Ctx ? [ctx?: Ctx] : [ctx: Ctx];

type RequestSlot<U> = {} extends UseCaseRequest<U> ? UseCaseRequest<U> | undefined : UseCaseRequest<U>;

export type ExecuteArgs<U, Ctx> = undefined extends Ctx
  ? [...RequestArgs<U>, ctx?: Ctx]
  : [request: RequestSlot<U>, ctx: Ctx];

const methodName = (useCase: string): string => useCase.charAt(0).toLowerCase() + useCase.slice(1);

export class Sdk<Ctx, M extends UseCaseMap, G extends GatewayMap = {}> {
  constructor(
    private readonly useCases: ReadonlyMap<string, AnyUseCase>,
    private readonly gateways: ReadonlyMap<string, readonly string[]>,
    private readonly config: SdkConfig<Ctx>,
  ) {}

  /**
   * The gateways bound to one call context, e.g. the request's `H3Event`:
   * `const { auth, cart } = sdk.with(event); await auth.login({ email, password });`
   */
  with(...ctx: ContextArgs<Ctx>): Gateways<G> {
    const context = ctx[0] as Ctx;
    const bound: Record<string, Record<string, (request?: object) => Promise<unknown>>> = {};
    for (const [gateway, names] of this.gateways) {
      const methods: Record<string, (request?: object) => Promise<unknown>> = {};
      for (const name of names) {
        methods[methodName(name)] = (request) => this.run(name, request, context);
      }
      bound[gateway] = methods;
    }
    return bound as Gateways<G>;
  }

  /** Calls a use case by name; the gateways are the friendlier way in. */
  execute<N extends keyof M & string>(name: N, ...args: ExecuteArgs<M[N], Ctx>): Promise<UseCaseResult<M[N]>> {
    const [request, ctx] = args as unknown as [object | undefined, Ctx];
    return this.run(name, request, ctx) as Promise<UseCaseResult<M[N]>>;
  }

  /** Names of the use cases this SDK was built with. */
  names(): (keyof M & string)[] {
    return [...this.useCases.keys()];
  }

  has(name: string): name is keyof M & string {
    return this.useCases.has(name);
  }

  private async run(name: string, request: object | undefined, ctx: Ctx): Promise<unknown> {
    const useCase = this.useCases.get(name);
    if (!useCase) throw new Error(`Unknown use case '${name}'`);
    const { validators } = useCase;
    const mode = this.config.validation;

    const parts = splitRequest(useCase.method, useCase.path, request);
    const params = validatePart(name, 'params', validators.params, parts.params, mode) as Record<string, unknown>;
    const query = validatePart(name, 'query', validators.query, parts.query, mode) as Record<string, unknown>;
    const body = validatePart(name, 'body', validators.body, parts.body, mode);
    const sendsBody = validators.body !== undefined;

    let store: TokenStore | undefined;
    const tokenStore = () => (store ??= this.config.tokenStore(ctx));

    const headers: Record<string, string> = { accept: 'application/json', ...this.config.headers };
    // a multipart body gets its content type, with the boundary, from fetch itself
    if (sendsBody && !useCase.multipart) headers['content-type'] = 'application/json';
    if (useCase.authRequired) {
      const token = await tokenStore().getAccessToken();
      if (!token) throw new SdkAuthError(name);
      headers.authorization = `Bearer ${token}`;
    }

    const url = this.config.baseUrls[useCase.service] + fillPath(useCase.path, params) + toQueryString(query);
    let response: Response;
    try {
      response = await this.config.fetch(url, {
        method: useCase.method,
        headers,
        body: sendsBody ? (useCase.multipart ? toFormData(body) : JSON.stringify(body)) : undefined,
      });
    } catch (cause) {
      throw new SdkNetworkError(name, cause);
    }

    const payload = await readJson(response);
    if (!response.ok) throw new SdkHttpError(name, response.status, payload as ApiErrorBody | undefined);

    if (useCase.tokenEffect === 'set') {
      const token = (payload as { accessToken?: unknown } | undefined)?.accessToken;
      if (typeof token === 'string') await tokenStore().setAccessToken(token);
    } else if (useCase.tokenEffect === 'clear') {
      await tokenStore().setAccessToken(undefined);
    }
    return useCase.mapResponse ? useCase.mapResponse(payload) : payload;
  }
}

/** `undefined` for an empty body (204, 202) or one that is not JSON. */
async function readJson(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return undefined;
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

/** A field that is a `Blob` becomes a file part; an array adds one part per item, in order; other values are text. */
function toFormData(body: unknown): FormData {
  const form = new FormData();
  for (const [key, value] of Object.entries((body ?? {}) as Record<string, unknown>)) {
    for (const item of Array.isArray(value) ? value : [value]) {
      if (item === undefined || item === null) continue;
      form.append(key, item instanceof Blob ? item : String(item));
    }
  }
  return form;
}
