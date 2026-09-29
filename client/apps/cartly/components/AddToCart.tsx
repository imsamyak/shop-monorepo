'use client';

import Link from 'next/link';
import { useState } from 'react';
import { formatMoney } from '@/lib/format';
import {
  apiError,
  isUnauthorized,
  useAddToCartMutation,
  useGetCartQuery,
  useSetCartQuantityMutation,
} from '@/lib/store/api';
import { CheckIcon, MinusIcon, PlusIcon, TrashIcon } from './icons';

export interface SkuOption {
  skuId: number;
  label: string;
  price: number;
  available: boolean;
}

const MAX_QUANTITY = 20;

/**
 * The price, the option to pick and one button. It reads "Add to cart" until the picked option is in the cart; after
 * that it becomes "Add more" with − and + at its ends, working on the same cache as the header's cart count.
 */
export function AddToCart({ productId, options }: { productId: string; options: SkuOption[] }) {
  const firstAvailable = options.find((option) => option.available);
  const [skuId, setSkuId] = useState(firstAvailable?.skuId ?? options[0]?.skuId);
  const [add, adding] = useAddToCartMutation();
  const [setQuantity, changing] = useSetCartQuantityMutation();
  // signed-out visitors get a 401 here, which simply means nothing is in a cart
  const { data: cart } = useGetCartQuery();

  const selected = options.find((option) => option.skuId === skuId);
  const lowest = Math.min(...options.map((option) => option.price));
  const showOptions = options.length > 1 || (options[0] && options[0].label !== 'Standard');
  const inCart = cart?.items.find((item) => item.skuId === skuId)?.quantity ?? 0;
  const busy = adding.isLoading || changing.isLoading;
  const error = adding.error ?? changing.error;

  if (!firstAvailable) {
    return (
      <div className="buy">
        <p className="price big">{formatMoney(lowest)}</p>
        <p className="badge out">Out of stock</p>
      </div>
    );
  }

  const change = (quantity: number) => {
    if (skuId && cart) setQuantity({ skuId, quantity, version: cart.version });
  };

  return (
    <div className="buy">
      <p className="price big">{formatMoney(selected?.price ?? lowest)}</p>

      {showOptions && (
        <fieldset className="options">
          <legend>
            Option <span className="muted">{selected?.label}</span>
          </legend>
          <div className="chips">
            {options.map((option) => (
              <label key={option.skuId} className={`pill${option.available ? '' : ' disabled'}`}>
                <input
                  type="radio"
                  name="skuId"
                  value={option.skuId}
                  checked={option.skuId === skuId}
                  disabled={!option.available}
                  onChange={() => {
                    adding.reset();
                    changing.reset();
                    setSkuId(option.skuId);
                  }}
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
        </fieldset>
      )}

      {inCart > 0 ? (
        <div className="merged" role="group" aria-label="Quantity in your cart">
          <button type="button" aria-label={inCart === 1 ? 'Remove from cart' : 'Remove one'} disabled={busy} onClick={() => change(inCart - 1)}>
            {inCart === 1 ? <TrashIcon /> : <MinusIcon />}
          </button>
          <span className="merged-label">
            <strong>Add more</strong>
            <small>{inCart} in cart</small>
          </span>
          <button type="button" aria-label="Add one more" disabled={busy || inCart >= MAX_QUANTITY} onClick={() => change(inCart + 1)}>
            <PlusIcon />
          </button>
        </div>
      ) : (
        <button
          type="button"
          className="primary wide"
          disabled={busy || !skuId || !selected?.available}
          onClick={() => skuId && add({ skuId, quantity: 1 })}
        >
          {adding.isLoading ? 'Adding…' : 'Add to cart'}
        </button>
      )}

      {inCart > 0 && (
        <p className="success note">
          <CheckIcon /> In your cart. <Link href="/cart">View cart</Link>
        </p>
      )}
      {error &&
        (isUnauthorized(error) ? (
          <p className="error note">
            <Link href={`/login?next=${encodeURIComponent(`/products/${productId}`)}`}>Sign in</Link> to add items to your cart.
          </p>
        ) : (
          <p className="error note">{apiError(error)}</p>
        ))}
    </div>
  );
}
