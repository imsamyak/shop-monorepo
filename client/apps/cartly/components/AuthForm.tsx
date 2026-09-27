'use client';

import Link from 'next/link';
import { useActionState } from 'react';
import type { FormState } from '@/lib/actions';

interface Props {
  action: (state: FormState, formData: FormData) => Promise<FormState>;
  mode: 'login' | 'register';
  next: string;
}

const field =
  'w-full rounded-xl border border-transparent bg-field px-4 py-3 text-base outline-none transition focus:border-accent focus:bg-surface focus:ring-4 focus:ring-accent/20';

export function AuthForm({ action, mode, next }: Props) {
  const [state, formAction, pending] = useActionState(action, {});
  const isLogin = mode === 'login';
  const other = `${isLogin ? '/register' : '/login'}?next=${encodeURIComponent(next)}`;

  return (
    <form action={formAction} className="flex w-full max-w-sm flex-col gap-4">
      <div>
        <h1 className="mb-1 text-3xl font-extrabold tracking-tight">{isLogin ? 'Welcome back' : 'Create your account'}</h1>
        <p className="text-muted">{isLogin ? 'Sign in to see your cart and orders.' : 'It takes a few seconds.'}</p>
      </div>
      <input type="hidden" name="next" value={next} />
      <label className="flex flex-col gap-1.5 font-semibold">
        Email
        <input name="email" type="email" autoComplete="email" required maxLength={254} placeholder="you@example.com" className={field} />
      </label>
      <label className="flex flex-col gap-1.5 font-semibold">
        Password
        <input
          name="password"
          type="password"
          autoComplete={isLogin ? 'current-password' : 'new-password'}
          required
          maxLength={200}
          placeholder={isLogin ? 'Your password' : 'At least 8 characters'}
          className={field}
        />
      </label>
      {state.error && (
        <p role="alert" className="rounded-xl bg-danger/10 px-4 py-2.5 text-sm font-medium text-danger">
          {state.error}
        </p>
      )}
      <button
        disabled={pending}
        className="rounded-xl border-accent bg-accent px-4 py-3 text-base font-bold text-white transition hover:bg-accent-hover disabled:opacity-60"
      >
        {pending ? 'Please wait…' : isLogin ? 'Sign in' : 'Create account'}
      </button>
      <p className="text-center text-muted">
        {isLogin ? 'New here? ' : 'Already have an account? '}
        <Link href={other} className="font-semibold">
          {isLogin ? 'Create an account' : 'Sign in'}
        </Link>
      </p>
    </form>
  );
}
