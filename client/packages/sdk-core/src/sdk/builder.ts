import type { AnyUseCase, UseCaseMap } from '../core/types.js';
import { type GatewayMap, Sdk, type SdkOptions } from './sdk.js';

type Simplify<T> = { [K in keyof T]: T[K] } & {};

type Keyed<L extends readonly AnyUseCase[]> = { [U in L[number] as U['name']]: U };

/**
 * Collects use cases, optionally grouped into named gateways, and builds an `Sdk` that knows each one's request and
 * response types. Every call returns a new builder, so a partly built one can be shared and extended.
 */
export class SdkBuilder<Ctx, M extends UseCaseMap = {}, G extends GatewayMap = {}> {
  private constructor(
    private readonly options: SdkOptions<Ctx>,
    private readonly useCases: ReadonlyMap<string, AnyUseCase>,
    private readonly gateways: ReadonlyMap<string, readonly string[]>,
  ) {}

  /** `Ctx` is what the token store provider receives on each call; leave it out when there is none. */
  static create<Ctx = undefined>(options: SdkOptions<Ctx>): SdkBuilder<Ctx> {
    return new SdkBuilder(options, new Map(), new Map());
  }

  /** Adds a use case reachable through `execute` only. */
  addUseCase<U extends AnyUseCase>(useCase: U): SdkBuilder<Ctx, Simplify<M & { [K in U['name']]: U }>, G> {
    return this.addUseCases([useCase]) as SdkBuilder<Ctx, Simplify<M & { [K in U['name']]: U }>, G>;
  }

  addUseCases<const L extends readonly AnyUseCase[]>(useCases: L): SdkBuilder<Ctx, Simplify<M & Keyed<L>>, G> {
    const next = new Map(this.useCases);
    for (const useCase of useCases) {
      if (next.has(useCase.name)) throw new Error(`Use case '${useCase.name}' is already added`);
      next.set(useCase.name, useCase);
    }
    return new SdkBuilder(this.options, next, this.gateways);
  }

  /**
   * Groups use cases under `name`, so `sdk.with(ctx)[name]` has a method for each. A use case may sit in more than one
   * gateway.
   */
  addGateway<const K extends string, const L extends readonly AnyUseCase[]>(
    name: K,
    useCases: L,
  ): SdkBuilder<Ctx, Simplify<M & Keyed<L>>, Simplify<G & { [k in K]: L }>> {
    if (this.gateways.has(name)) throw new Error(`Gateway '${name}' is already added`);
    const next = new Map(this.useCases);
    for (const useCase of useCases) {
      const existing = next.get(useCase.name);
      if (existing && existing !== useCase) throw new Error(`Another use case named '${useCase.name}' is already added`);
      next.set(useCase.name, useCase);
    }
    const gateways = new Map(this.gateways).set(
      name,
      useCases.map((useCase) => useCase.name),
    );
    return new SdkBuilder(this.options, next, gateways);
  }

  build(): Sdk<Ctx, M, G> {
    const fetchImpl = this.options.fetch ?? globalThis.fetch;
    if (!fetchImpl) throw new Error('No fetch available: pass options.fetch');
    return new Sdk(this.useCases, this.gateways, {
      tokenStore: this.options.tokenStore,
      baseUrls: this.options.baseUrls,
      validation: this.options.validation ?? 'shape',
      fetch: fetchImpl.bind(globalThis),
      headers: this.options.headers ?? {},
    });
  }
}
