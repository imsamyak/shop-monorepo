'use client';

import { logout } from '@/lib/actions';
import { shopApi } from '@/lib/store/api';
import { useAppDispatch } from '@/lib/store/store';

export function LogoutButton({ email }: { email: string }) {
  const dispatch = useAppDispatch();
  return (
    <form action={logout}>
      {/* the next buyer on this browser must not see this buyer's cached cart */}
      <button className="link" title={email} onClick={() => dispatch(shopApi.util.resetApiState())}>
        Sign out
      </button>
    </form>
  );
}
