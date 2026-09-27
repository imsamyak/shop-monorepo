'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect } from 'react';
import { useGetCartQuery } from '@/lib/store/api';
import { CartIcon } from './icons';

/** The header's cart link with the number of items, kept current by the cart's cache. */
export function CartLink({ signedIn }: { signedIn: boolean }) {
  const pathname = usePathname();
  const { data, refetch } = useGetCartQuery(undefined, { skip: !signedIn });

  // an order is placed through a server action, which empties the cart without telling the cache
  useEffect(() => {
    if (signedIn) refetch();
  }, [pathname, signedIn, refetch]);

  const count = signedIn ? (data?.count ?? 0) : 0;
  return (
    <Link href="/cart" className="nav-cart" aria-label={count ? `Cart, ${count} items` : 'Cart'}>
      <CartIcon />
      <span>Cart</span>
      {count > 0 && <span className="count">{count}</span>}
    </Link>
  );
}
