'use client';

import { usePathname, useRouter } from 'next/navigation';
import { type FormEvent, useEffect, useState } from 'react';
import { setSearch } from '@/lib/store/browse';
import { useAppDispatch, useAppSelector } from '@/lib/store/store';
import { SearchIcon } from './icons';

/** Searching from any page ends up on the home page, whose product list reads the same search from the store. */
export function HeaderSearch() {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const pathname = usePathname();
  const stored = useAppSelector((state) => state.browse.search);
  const [text, setText] = useState(stored);

  // the home page's "clear filters" empties the stored search; the box follows
  useEffect(() => setText(stored), [stored]);

  // on the home page the list follows the typing, shortly after the last key
  useEffect(() => {
    if (pathname !== '/' || text.trim() === stored) return;
    const timer = setTimeout(() => dispatch(setSearch(text)), 350);
    return () => clearTimeout(timer);
  }, [text, stored, pathname, dispatch]);

  function submit(event: FormEvent) {
    event.preventDefault();
    dispatch(setSearch(text));
    if (pathname !== '/') router.push('/');
  }

  return (
    <form className="search" role="search" onSubmit={submit}>
      <SearchIcon />
      <input
        type="search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Search products"
        aria-label="Search products"
        maxLength={100}
      />
    </form>
  );
}
