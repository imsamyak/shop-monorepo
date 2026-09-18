'use client';

import Link from 'next/link';
import { useActionState } from 'react';
import type { FormState } from '@/lib/actions';

interface Props {
  action: (state: FormState, formData: FormData) => Promise<FormState>;
  mode: 'login' | 'register' | 'seller';
  next: string;
}

export function AuthForm({ action, mode, next }: Props) {
  const [state, formAction, pending] = useActionState(action, {});
  const isLogin = mode === 'login';
  const isSeller = mode === 'seller';
  const other = `${isLogin ? '/register' : '/login'}?next=${encodeURIComponent(next)}`;

  return (
    <form action={formAction} className="card auth">
      <h1>{isLogin ? 'Sign in' : isSeller ? 'Sell on Cartly' : 'Create an account'}</h1>
      <input type="hidden" name="next" value={next} />
      <label>
        Email
        <input name="email" type="email" autoComplete="email" required maxLength={254} />
      </label>
      <label>
        Password
        <input
          name="password"
          type="password"
          autoComplete={isLogin ? 'current-password' : 'new-password'}
          required
          maxLength={200}
        />
      </label>
      {state.error && <p className="error">{state.error}</p>}
      <button className="primary" disabled={pending}>
        {pending ? 'Please wait…' : isLogin ? 'Sign in' : isSeller ? 'Create seller account' : 'Create account'}
      </button>
      <p className="muted">
        {isLogin ? 'New here? ' : 'Already have an account? '}
        <Link href={other}>{isLogin ? 'Create an account' : 'Sign in'}</Link>
      </p>
    </form>
  );
}
