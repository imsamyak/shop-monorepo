'use client';

import Link from 'next/link';
import { useState } from 'react';
import { formatMoney } from '@/lib/format';
import { apiError, isUnauthorized, useAddToCartMutation } from '@/lib/store/api';
import { CheckIcon } from './icons';

export interface SkuOption {
  skuId: number;
  label: string;
  price: number;
  available: boolean;
}

/** The price, the option to pick, the quantity and the button. The cart in the header updates as soon as the add lands. */
export function AddToCart({ productId, options }: { productId: string; options: SkuOption[] }) {
  const firstAvailable = options.find((option) => option.available);
  const [skuId, setSkuId] = useState(firstAvailable?.skuId ?? options[0]?.skuId);
  const [quantity, setQuantity] = useState(1);
  const [add, { isLoading, isSuccess, error, reset }] = useAddToCartMutation();

  const selected = options.find((option) => option.skuId === skuId);
  const lowest = Math.min(...options.map((option) => option.price));
  const showOptions = options.length > 1 || (options[0] && options[0].label !== 'Standard');

  if (!firstAvailable) {
    return (
      <div className="buy">
        <p className="price big">{formatMoney(lowest)}</p>
        <p className="badge out">Out of stock</p>
      </div>
    );
  }

  const change = (next: number) => {
    reset();
    setQuantity(Math.min(20, Math.max(1, next)));
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
                    reset();
                    setSkuId(option.skuId);
                  }}
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
        </fieldset>
      )}

      <div className="row buy-row">
        <div className="stepper" role="group" aria-label="Quantity">
          <button type="button" onClick={() => change(quantity - 1)} disabled={quantity <= 1} aria-label="Fewer">
            −
          </button>
          <output>{quantity}</output>
          <button type="button" onClick={() => change(quantity + 1)} disabled={quantity >= 20} aria-label="More">
            +
          </button>
        </div>
        <button
          type="button"
          className="primary grow"
          disabled={isLoading || !skuId || !selected?.available}
          onClick={() => skuId && add({ skuId, quantity })}
        >
          {isLoading ? 'Adding…' : 'Add to cart'}
        </button>
      </div>

      {isSuccess && (
        <p className="success note">
          <CheckIcon /> Added to your cart. <Link href="/cart">View cart</Link>
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
