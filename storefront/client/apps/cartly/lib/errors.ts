import { SdkHttpError, SdkNetworkError, SdkValidationError } from '@shop/platform-sdk';

/** A message a buyer can read, from any error the SDK throws. */
export function errorMessage(error: unknown): string {
  if (error instanceof SdkValidationError || (error instanceof SdkHttpError && error.status === 400)) {
    const fields = Object.entries(error.invalidFields).map(([field, message]) => `${field} ${message}`);
    if (fields.length) return fields.join(', ');
  }
  if (error instanceof SdkHttpError) {
    if (error.status === 401) return 'Wrong email or password.';
    if (error.status === 409) return 'This changed in the meantime. Please try again.';
    return error.body?.detail ?? error.body?.title ?? `Something went wrong (HTTP ${error.status}).`;
  }
  if (error instanceof SdkNetworkError) return 'The shop is not reachable right now.';
  return 'Something went wrong.';
}
