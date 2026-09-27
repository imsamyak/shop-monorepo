'use client';

import { useState } from 'react';
import { useListProductsQuery, usePrefetch } from '@/lib/store/api';
import { clearFilters, setInStock, setPage, setSort } from '@/lib/store/browse';
import { useAppDispatch, useAppSelector } from '@/lib/store/store';
import { SORTS, type Sort } from '@/lib/types';
import { FilterIcon, SortIcon } from './icons';
import { ProductCard, ProductCardSkeleton } from './ProductCard';

const PAGE_SIZE = 12;
const DEFAULT_SORT: Sort = 'createdAt,desc';

/** The home page's products: the filters live in the store and each combination of them is cached on its own. */
export function ProductBrowser() {
  const dispatch = useAppDispatch();
  const { search, sort, inStock, page } = useAppSelector((state) => state.browse);
  const query = { search, sort, inStock, page, size: PAGE_SIZE };
  const { data, isFetching, isError, refetch } = useListProductsQuery(query);
  const prefetch = usePrefetch('listProducts');
  const [open, setOpen] = useState(false);
  const active = (inStock ? 1 : 0) + (sort !== DEFAULT_SORT ? 1 : 0);
  const filtered = search !== '' || active > 0;

  return (
    <section id="products" aria-busy={isFetching}>
      <div className="toolbar">
        <div>
          <h2>{search ? `Results for “${search}”` : 'All products'}</h2>
          <p className="muted tagline">
            {data ? `${data.totalElements} ${data.totalElements === 1 ? 'product' : 'products'}` : 'Loading…'}
          </p>
        </div>
        <button
          type="button"
          className={`filter-button${open ? ' open' : ''}`}
          aria-expanded={open}
          aria-controls="filter-panel"
          onClick={() => setOpen((value) => !value)}
        >
          <FilterIcon />
          Filters
          {active > 0 && <span className="count">{active}</span>}
        </button>
      </div>

      <div id="filter-panel" className={`filter-panel${open ? ' open' : ''}`} inert={!open}>
        <div className="filter-inner">
          <div className="filter-groups">
            <div className="filter-group">
              <h3>
                <FilterIcon size={16} /> Availability
              </h3>
              <label className="toggle">
                <input
                  type="checkbox"
                  role="switch"
                  checked={inStock}
                  onChange={(e) => dispatch(setInStock(e.target.checked))}
                />
                <span className="track" aria-hidden />
                In stock only
              </label>
            </div>
            <fieldset className="filter-group">
              <legend>
                <SortIcon /> Sort by
              </legend>
              <div className="chips">
                {Object.entries(SORTS).map(([value, label]) => (
                  <label key={value} className="pill">
                    <input
                      type="radio"
                      name="sort"
                      value={value}
                      checked={sort === value}
                      onChange={() => dispatch(setSort(value as Sort))}
                    />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          </div>
          <div className="filter-actions">
            <button type="button" className="link" onClick={() => dispatch(clearFilters())} disabled={!filtered}>
              Clear all
            </button>
            <button type="button" className="primary small" onClick={() => setOpen(false)}>
              Show {data ? data.totalElements : ''} products
            </button>
          </div>
        </div>
      </div>

      {isError ? (
        <div className="empty">
          <p>We could not load the products.</p>
          <button type="button" onClick={refetch}>
            Try again
          </button>
        </div>
      ) : !data ? (
        <ul className="grid">
          {Array.from({ length: PAGE_SIZE }, (_, i) => (
            <li key={i}>
              <ProductCardSkeleton />
            </li>
          ))}
        </ul>
      ) : data.items.length === 0 ? (
        <p className="empty">Nothing matches that. Try another search or clear the filters.</p>
      ) : (
        <ul className={`grid${isFetching ? ' fetching' : ''}`}>
          {data.items.map((product) => (
            <li key={product.id}>
              <ProductCard product={product} />
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <nav className="pager" aria-label="Pages">
          <button type="button" disabled={page === 0} onClick={() => dispatch(setPage(page - 1))}>
            ← Previous
          </button>
          <span className="muted">
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= data.totalPages}
            onMouseEnter={() => prefetch({ ...query, page: page + 1 })}
            onClick={() => dispatch(setPage(page + 1))}
          >
            Next →
          </button>
        </nav>
      )}
    </section>
  );
}
