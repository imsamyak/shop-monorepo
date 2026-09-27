'use client';

import { setupListeners } from '@reduxjs/toolkit/query';
import { useEffect, useRef } from 'react';
import { Provider } from 'react-redux';
import { type AppStore, makeStore } from '@/lib/store/store';

export function StoreProvider({ children }: { children: React.ReactNode }) {
  const store = useRef<AppStore>(null);
  store.current ??= makeStore();

  // refetch the cart and lists when the tab regains focus or the connection returns
  useEffect(() => (store.current ? setupListeners(store.current.dispatch) : undefined), []);

  return <Provider store={store.current}>{children}</Provider>;
}
