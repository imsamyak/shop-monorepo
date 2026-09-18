import { SdkHttpError, type SkuResponse } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { addImages, addSku, addStock, moveImage, removeImage, saveDetails } from '@/lib/seller-actions';
import { formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireSeller } from '@/lib/session';

export const metadata = { title: 'Manage product' };

async function load(id: string) {
  try {
    const [product, skus] = await Promise.all([
      api().products.getProduct({ id }),
      api().products.listSkus({ productId: id }),
    ]);
    return { product, skus };
  } catch (error) {
    if (error instanceof SdkHttpError && (error.status === 404 || error.status === 400)) notFound();
    throw error;
  }
}

const optionLabel = (sku: SkuResponse) => {
  const entries = Object.entries(sku.properties);
  return entries.length ? entries.map(([name, value]) => `${name}: ${value}`).join(', ') : 'Standard';
};

/** The stock of a SKU, or null while inventory has not heard of it yet (it learns of new SKUs a moment later). */
async function stockOf(skuId: number) {
  try {
    return await api().inventory.getInventory({ skuId });
  } catch {
    return null;
  }
}

export default async function ManageProductPage({ params, searchParams }: PageProps<'/seller/products/[id]'>) {
  const { id } = await params;
  const { error } = await searchParams;
  const user = await requireSeller(`/seller/products/${id}`);
  const { product, skus } = await load(id);
  if (product.sellerId !== user.sellerId) notFound();
  const stock = await Promise.all(skus.map((sku) => stockOf(sku.skuId)));

  return (
    <>
      <Link href="/seller" className="back">
        ← Your products
      </Link>
      <h1>{product.title}</h1>
      {typeof error === 'string' && <p className="error card">{error}</p>}

      <section className="card stack">
        <h2>Details</h2>
        <form action={saveDetails} className="stack">
          <input type="hidden" name="productId" value={product.id} />
          <label>
            Title
            <input name="title" defaultValue={product.title} required minLength={5} maxLength={50} />
          </label>
          <label>
            Description
            <textarea name="description" defaultValue={product.description ?? ''} maxLength={200} rows={3} />
          </label>
          <p className="muted">Base price {formatMoney(product.defaultPrice)}</p>
          <button className="primary">Save details</button>
        </form>
      </section>

      <section className="card stack">
        <h2>Pictures</h2>
        <p className="muted">The first picture is the main one. Move pictures to change the order.</p>
        {product.images.length === 0 ? (
          <p className="muted">No pictures yet.</p>
        ) : (
          <ol className="pictures">
            {product.images.map((url, position) => (
              <li key={url} className="picture">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={url} alt="" />
                <span className="muted">{position === 0 ? 'Main' : `#${position + 1}`}</span>
                <div className="row">
                  {([-1, 1] as const).map((direction) => (
                    <form action={moveImage} key={direction}>
                      <input type="hidden" name="productId" value={product.id} />
                      <input type="hidden" name="position" value={position} />
                      <input type="hidden" name="direction" value={direction} />
                      <button
                        disabled={direction === -1 ? position === 0 : position === product.images.length - 1}
                        aria-label={direction === -1 ? 'Move earlier' : 'Move later'}
                      >
                        {direction === -1 ? '←' : '→'}
                      </button>
                    </form>
                  ))}
                  <form action={removeImage}>
                    <input type="hidden" name="productId" value={product.id} />
                    <input type="hidden" name="url" value={url} />
                    <button className="link danger">Remove</button>
                  </form>
                </div>
              </li>
            ))}
          </ol>
        )}
        {product.images.length < 10 && (
          <form action={addImages} className="row">
            <input type="hidden" name="productId" value={product.id} />
            <input name="images" type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple required />
            <button>Add pictures</button>
          </form>
        )}
      </section>

      <section className="card stack">
        <h2>Options and stock</h2>
        {skus.length === 0 ? (
          <p className="muted">No options yet. Add one below so buyers can order this product.</p>
        ) : (
          <ul className="orders">
            {skus.map((sku, i) => (
              <li key={sku.skuId} className="row sku-row">
                <span className="grow">
                  {optionLabel(sku)} · {formatMoney(sku.price)}
                </span>
                <span className="muted">
                  {stock[i] ? `${stock[i].availableQuantity} available (${stock[i].totalQuantity} in stock)` : 'stock not ready yet'}
                </span>
                <form action={addStock} className="row">
                  <input type="hidden" name="productId" value={product.id} />
                  <input type="hidden" name="skuId" value={sku.skuId} />
                  <input name="quantity" type="number" min={1} max={1000000} defaultValue={10} aria-label="Quantity" className="qty-input" />
                  <button>Add stock</button>
                </form>
              </li>
            ))}
          </ul>
        )}
        <form action={addSku} className="stack">
          <h3>Add an option</h3>
          <input type="hidden" name="productId" value={product.id} />
          <div className="row">
            <input name="optionName" placeholder="Option (e.g. color)" maxLength={50} />
            <input name="optionValue" placeholder="Value (e.g. red)" maxLength={50} />
            <input name="price" type="number" min={0} step="0.01" placeholder="Price (optional)" />
          </div>
          <p className="muted">Leave the option empty for a single standard version. Without a price it uses the base price.</p>
          <button className="primary">Add option</button>
        </form>
      </section>
    </>
  );
}
