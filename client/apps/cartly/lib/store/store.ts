import { configureStore } from '@reduxjs/toolkit';
import { useDispatch, useSelector } from 'react-redux';
import { shopApi } from './api';
import { browseReducer } from './browse';

/** A store per browser tab (made once by `StoreProvider`), never one shared between requests on the server. */
export const makeStore = () =>
  configureStore({
    reducer: {
      [shopApi.reducerPath]: shopApi.reducer,
      browse: browseReducer,
    },
    middleware: (getDefault) => getDefault().concat(shopApi.middleware),
  });

export type AppStore = ReturnType<typeof makeStore>;
export type RootState = ReturnType<AppStore['getState']>;
export type AppDispatch = AppStore['dispatch'];

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppSelector = useSelector.withTypes<RootState>();
