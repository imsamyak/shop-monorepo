'use client';

import Link from 'next/link';
import { startCheckout } from '@/lib/actions';
import { formatMoney } from '@/lib/format';
import {
  apiError,
  isUnauthorized,
  useGetCartQuery,
  useRemoveCartItemMutation,
  useSetCartQuantityMutation,
} from '@/lib/store/api';
import { BagIcon, TrashIcon } from './icons';

/** The cart page. Changes show at once and are sent to the server; the badge in the header reads the same cache. */
export function CartView({ error }: { error?: string }) {
  const { data: cart, isLoading, error: loadError, refetch } = useGetCartQuery();
  const [setQuantity, quantityResult] = useSetCartQuantityMutation();
  const [remove, removeResult] = useRemoveCartItemMutation();
  const problem = loadError ?? quantityResult.error ?? removeResult.error;

  if (isLoading) {
    return (
      <div className="cart-layout" aria-busy>
        <div className="cart">
          {[0, 1, 2].map((i) => (
            <div key={i} className="card cart-line skeleton">
              <div className="thumb-lg" />
              <div className="grow">
                <span className="line" />
                <span className="line short" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (loadError && !cart) {
    return (
      <div className="empty">
        {isUnauthorized(loadError) ? (
          <p>
            <Link href="/login?next=%2Fcart">Sign in</Link> to see your cart.
          </p>
        ) : (
          <>
            <p>{apiError(loadError)}</p>
            <button type="button" onClick={refetch}>
              Try again
            </button>
          </>
        )}
      </div>
    );
  }

  if (!cart || cart.items.length === 0) {
    return (
      <div className="empty-state">
        <BagIcon size={44} />
        <h2>Your cart is empty</h2>
        <p className="muted">Add something you like and it will wait for you here.</p>
        <Link href="/" className="button primary">
          Start shopping
        </Link>
      </div>
    );
  }

  const busy = quantityResult.isLoading || removeResult.isLoading;
  const unavailable = cart.items.some((item) => !item.available);

  return (
    <div className="cart-layout">
      <div>
        {(error || problem) && <p className="error note">{error ?? apiError(problem)}</p>}
        <ul className="cart">
          {cart.items.map((item) => (
            <li key={item.skuId} className="card cart-line">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={item.image} alt="" />
              <div className="grow">
                {item.productId ? <Link href={`/products/${item.productId}`}>{item.title}</Link> : <span>{item.title}</span>}
                <div className="muted">
                  {item.label && `${item.label} · `}
                  {formatMoney(item.unitPrice)}
                </div>
                {!item.available && <span className="badge out">Out of stock</span>}
              </div>
              <div className="stepper" role="group" aria-label={`Quantity of ${item.title}`}>
                <button
                  type="button"
                  aria-label="Fewer"
                  disabled={busy || item.quantity <= 1}
                  onClick={() => setQuantity({ skuId: item.skuId, quantity: item.quantity - 1, version: cart.version })}
                >
                  −
                </button>
                <output>{item.quantity}</output>
                <button
                  type="button"
                  aria-label="More"
                  disabled={busy || item.quantity >= 20}
                  onClick={() => setQuantity({ skuId: item.skuId, quantity: item.quantity + 1, version: cart.version })}
                >
                  +
                </button>
              </div>
              <strong className="line-total">{formatMoney(item.unitPrice * item.quantity)}</strong>
              <button
                type="button"
                className="icon-button"
                aria-label={`Remove ${item.title}`}
                disabled={busy}
                onClick={() => remove({ skuId: item.skuId, version: cart.version })}
              >
                <TrashIcon />
              </button>
            </li>
          ))}
        </ul>
      </div>

      <aside className="summary card">
        <h2>Order summary</h2>
        <div className="sum-row">
          <span className="muted">Items ({cart.count})</span>
          <span>{formatMoney(cart.total)}</span>
        </div>
        <div className="sum-row total">
          <span>Estimated total</span>
          <strong>{formatMoney(cart.total)}</strong>
        </div>
        <p className="muted small">Checkout prices your order for real and holds the prices for a while.</p>
        <form action={startCheckout}>
          <button className="primary wide" disabled={busy || unavailable}>
            Checkout
          </button>
        </form>
        {unavailable && <p className="error small">Remove the items that are out of stock to check out.</p>}
      </aside>
    </div>
  );
}
