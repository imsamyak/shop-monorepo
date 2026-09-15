import { SdkValidationError } from './errors.js';
import type { RequestPart, ValidationIssue, Validator } from './types.js';

/**
 * How much of a body is checked before sending. Params and query are always checked in full because they build the URL.
 * - `strict`: every rule, limits included.
 * - `shape`: required fields, types, formats and unknown keys; numeric and length limits are left to the backend, so a
 *   limit changed there does not need an SDK release.
 * - `off`: the body is sent as given.
 */
export type ValidationMode = 'strict' | 'shape' | 'off';

/**
 * Upper bounds, and lower bounds above 1. Bounds of 0 or 1 (`nonnegative`, `positive`, non-empty) say what kind of
 * value a field is rather than how big it may be, so `shape` keeps them.
 */
function isLimit(issue: ValidationIssue): boolean {
  return issue.code === 'too_big' || (issue.code === 'too_small' && Number(issue.minimum ?? 0) > 1);
}

/** `items[0].quantity`, as the backend names fields. */
function fieldName(part: RequestPart, path: readonly PropertyKey[]): string {
  if (path.length === 0) return part;
  return path
    .map((segment, i) => (typeof segment === 'number' ? `[${segment}]` : `${i === 0 ? '' : '.'}${String(segment)}`))
    .join('');
}

/**
 * Returns the value to send for `part`, or throws `SdkValidationError`. A part without a validator takes no fields, so
 * any it was given are unknown.
 */
export function validatePart(
  useCase: string,
  part: RequestPart,
  validator: Validator<unknown> | undefined,
  value: Record<string, unknown>,
  mode: ValidationMode,
): unknown {
  if (!validator) {
    const unknown = Object.keys(value);
    if (unknown.length === 0) return value;
    throw new SdkValidationError(useCase, part, Object.fromEntries(unknown.map((key) => [key, 'Unrecognized key'])));
  }
  const effective = part === 'body' ? mode : 'strict';
  if (effective === 'off') return value;

  const outcome = validator.safeParse(value);
  if (outcome.success) return outcome.data;

  const issues = effective === 'shape' ? outcome.error.issues.filter((issue) => !isLimit(issue)) : outcome.error.issues;
  if (issues.length === 0) return value;

  const invalidFields: Record<string, string> = {};
  for (const issue of issues) {
    invalidFields[fieldName(part, issue.path)] ??= issue.message;
  }
  throw new SdkValidationError(useCase, part, invalidFields);
}
