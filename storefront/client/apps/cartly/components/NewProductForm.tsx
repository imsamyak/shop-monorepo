'use client';

import { useActionState } from 'react';
import { createProduct } from '@/lib/seller-actions';

export function NewProductForm() {
  const [state, formAction, pending] = useActionState(createProduct, {});

  return (
    <form action={formAction} className="card stack">
      <h1>New product</h1>
      <label>
        Title
        <input name="title" required minLength={5} maxLength={50} />
      </label>
      <label>
        Description
        <textarea name="description" required maxLength={200} rows={3} />
      </label>
      <label>
        Price
        <input name="defaultPrice" type="number" min={1} step="0.01" required />
      </label>
      <label>
        Pictures <span className="muted">(up to 10; the first is the main one, in the order you choose them)</span>
        <input name="images" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple />
      </label>
      {state.error && <p className="error">{state.error}</p>}
      <button className="primary" disabled={pending}>
        {pending ? 'Saving…' : 'Create product'}
      </button>
      <p className="muted">After this you can add options (SKUs) and stock.</p>
    </form>
  );
}
