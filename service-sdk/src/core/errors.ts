import type { RequestPart } from './types.js';

/** The backend's error body (a Spring `ProblemDetail` plus the shared exception handler's properties). */
export interface ApiErrorBody {
  readonly type?: string;
  readonly title?: string;
  readonly status?: number;
  readonly detail?: string;
  readonly instance?: string;
  readonly errorCode?: string;
  readonly timestamp?: string;
  /** `{field: message}`, on 400 validation errors. */
  readonly invalidFields?: Record<string, string>;
  readonly details?: unknown;
}

export class SdkError extends Error {
  constructor(
    message: string,
    readonly useCase: string,
    options?: { cause?: unknown },
  ) {
    super(message, options);
    this.name = new.target.name;
  }
}

/** The input failed the use case's validators; nothing was sent. `invalidFields` has the backend's 400 shape. */
export class SdkValidationError extends SdkError {
  constructor(
    useCase: string,
    readonly part: RequestPart,
    readonly invalidFields: Record<string, string>,
  ) {
    super(`${useCase}: invalid ${part}`, useCase);
  }
}

/** The use case needs a token and the token store had none; nothing was sent. */
export class SdkAuthError extends SdkError {
  constructor(useCase: string) {
    super(`${useCase}: an access token is required`, useCase);
  }
}

/** The backend answered with a non-2xx status. */
export class SdkHttpError extends SdkError {
  constructor(
    useCase: string,
    readonly status: number,
    readonly body: ApiErrorBody | undefined,
  ) {
    super(`${useCase}: HTTP ${status}${body?.detail ? ` - ${body.detail}` : ''}`, useCase);
  }

  get errorCode(): string | undefined {
    return this.body?.errorCode;
  }

  get invalidFields(): Record<string, string> {
    return this.body?.invalidFields ?? {};
  }
}

/** The request never got a response (connection refused, DNS, aborted, ...). */
export class SdkNetworkError extends SdkError {
  constructor(useCase: string, cause: unknown) {
    super(`${useCase}: request failed`, useCase, { cause });
  }
}
