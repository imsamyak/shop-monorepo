'use client';

import Link from 'next/link';
import { useActionState } from 'react';
import { addToCart } from '@/lib/actions';

export interface SkuOption {
  skuId: string;
  label: string;
  price: string;
  available: boolean;
}

export function AddToCart({ productId, options }: { productId: string; options: SkuOption[] }) {
  const [state, formAction, pending] = useActionState(addToCart, {});
  const firstAvailable = options.find((option) => option.available)?.skuId;

  if (!firstAvailable) return <p className="badge out">Out of stock</p>;

  return (
    <form action={formAction} className="add-to-cart">
      <input type="hidden" name="productId" value={productId} />
      <fieldset>
        <legend>Option</legend>
        {options.map((option) => (
          <label key={option.skuId} className={`option${option.available ? '' : ' disabled'}`}>
            <input
              type="radio"
              name="skuId"
              value={option.skuId}
              defaultChecked={option.skuId === firstAvailable}
              disabled={!option.available}
            />
            <span>{option.label}</span>
            <span className="muted">{option.available ? option.price : 'Out of stock'}</span>
          </label>
        ))}
      </fieldset>
      <div className="row">
        <label className="qty">
          Qty
          <input name="quantity" type="number" min={1} max={1000} defaultValue={1} required />
        </label>
        <button className="primary" disabled={pending}>
          {pending ? 'Adding…' : 'Add to cart'}
        </button>
      </div>
      {state.error && <p className="error">{state.error}</p>}
      {state.message && (
        <p className="success">
          {state.message} <Link href="/cart">View cart</Link>
        </p>
      )}
    </form>
  );
}
