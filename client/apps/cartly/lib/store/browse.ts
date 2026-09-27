import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { Sort } from '../types';

/** What the buyer is looking for on the home page. The header's search box and the filters below it share this. */
export interface BrowseState {
  search: string;
  sort: Sort;
  inStock: boolean;
  page: number;
}

const initialState: BrowseState = { search: '', sort: 'createdAt,desc', inStock: false, page: 0 };

const browse = createSlice({
  name: 'browse',
  initialState,
  reducers: {
    setSearch(state, action: PayloadAction<string>) {
      state.search = action.payload.trim();
      state.page = 0;
    },
    setSort(state, action: PayloadAction<Sort>) {
      state.sort = action.payload;
      state.page = 0;
    },
    setInStock(state, action: PayloadAction<boolean>) {
      state.inStock = action.payload;
      state.page = 0;
    },
    setPage(state, action: PayloadAction<number>) {
      state.page = Math.max(0, action.payload);
    },
    clearFilters: () => initialState,
  },
});

export const { setSearch, setSort, setInStock, setPage, clearFilters } = browse.actions;
export const browseReducer = browse.reducer;
