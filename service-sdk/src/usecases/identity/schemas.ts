import { z } from 'zod';
import { requiredText } from '../shared/schemas.js';

export const email = requiredText(254);
export const password = requiredText(200);

export const credentialsBody = z.strictObject({ email, password });

export const forgotPasswordBody = z.strictObject({ email });

export const resetPasswordBody = z.strictObject({ token: requiredText(200), newPassword: password });
