import { useCase } from '@shop/sdk-core';
import { credentialsBody, forgotPasswordBody, resetPasswordBody } from './schemas.js';
import type { SessionResponse, UserResponse } from './types.js';

export type * from './types.js';

export const registerBuyer = useCase('RegisterBuyer')
  .service('identity')
  .post('/api/auth/register')
  .body(credentialsBody)
  .storesToken()
  .returns<SessionResponse>()
  .build();

/** Anyone can sign up as a seller; the response carries the seller id. */
export const registerSeller = useCase('RegisterSeller')
  .service('identity')
  .post('/api/auth/register/seller')
  .body(credentialsBody)
  .storesToken()
  .returns<SessionResponse>()
  .build();

export const login = useCase('Login')
  .service('identity')
  .post('/api/auth/login')
  .body(credentialsBody)
  .storesToken()
  .returns<SessionResponse>()
  .build();

/** Tokens are stateless, so this only clears the stored one. It sends none, so an expired session can still log out. */
export const logout = useCase('Logout')
  .service('identity')
  .post('/api/auth/logout')
  .clearsToken()
  .build();

export const getCurrentUser = useCase('GetCurrentUser')
  .service('identity')
  .get('/api/auth/me')
  .authRequired()
  .returns<UserResponse>()
  .build();

/** Always 202, whether or not the address has an account. */
export const forgotPassword = useCase('ForgotPassword')
  .service('identity')
  .post('/api/auth/forgot-password')
  .body(forgotPasswordBody)
  .build();

export const resetPassword = useCase('ResetPassword')
  .service('identity')
  .post('/api/auth/reset-password')
  .body(resetPasswordBody)
  .build();

/** Admin only. */
export const createSeller = useCase('CreateSeller')
  .service('identity')
  .post('/api/auth/admin/sellers')
  .body(credentialsBody)
  .authRequired()
  .returns<UserResponse>()
  .build();

export const identityUseCases = [
  registerBuyer,
  registerSeller,
  login,
  logout,
  getCurrentUser,
  forgotPassword,
  resetPassword,
  createSeller,
] as const;
