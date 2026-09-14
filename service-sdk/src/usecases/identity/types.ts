export type UserRole = 'ROLE_BUYER' | 'ROLE_SELLER' | 'ROLE_ADMIN' | 'ROLE_SYSTEM';

export interface UserResponse {
  id: string;
  email: string;
  role: UserRole;
  sellerId: string | null;
}

export interface SessionResponse {
  accessToken: string;
  tokenType: 'Bearer';
  /** Seconds. */
  expiresIn: number;
  user: UserResponse;
}
