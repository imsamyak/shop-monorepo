import Link from 'next/link';
import { removeItem, setQuantity, startCheckout } from '@/lib/actions';
import { getSkuIndex, skuLabel } from '@/lib/catalog';
import { formatMoney } from '@/lib/format';
import { mainImage } from '@/lib/images';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

export default async function CartPage({ searchParams }: PageProps<'/cart'>) {
  const user = await requireUser('/cart');
  const { error } = await searchParams;
  const [cart, index] = await Promise.all([api().cart.getCart({ customerId: user.id }), getSkuIndex()]);

  // An estimate from catalog prices; the checkout prices it for real.
  const estimate = cart.items.reduce((sum, item) => sum + (index.get(item.skuId)?.sku.price ?? 0) * item.quantity, 0);

  return (
    <>
      <h1>Your cart</h1>
      {typeof error === 'string' && <p className="error">{error}</p>}
      {cart.items.length === 0 ? (
        <p className="empty">
          Your cart is empty. <Link href="/">Start shopping</Link>
        </p>
      ) : (
        <>
          <ul className="cart">
            {cart.items.map((item) => {
              const info = index.get(item.skuId);
              return (
                <li key={item.skuId} className="card cart-line">
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  {info && <img src={mainImage(info.product.images)} alt="" />}
                  <div className="grow">
                    {info ? (
                      <>
                        <Link href={`/products/${info.product.id}`}>{info.product.title}</Link>
                        <div className="muted">
                          {skuLabel(info.sku)} · {formatMoney(info.sku.price)}
                        </div>
                      </>
                    ) : (
                      <span>SKU #{item.skuId}</span>
                    )}
                  </div>
                  <form action={setQuantity} className="row">
                    <input type="hidden" name="skuId" value={item.skuId} />
                    <input type="hidden" name="version" value={cart.version} />
                    <input
                      name="quantity"
                      type="number"
                      min={0}
                      max={1000}
                      defaultValue={item.quantity}
                      aria-label="Quantity"
                      className="qty-input"
                    />
                    <button>Update</button>
                  </form>
                  <form action={removeItem}>
                    <input type="hidden" name="skuId" value={item.skuId} />
                    <input type="hidden" name="version" value={cart.version} />
                    <button className="link danger">Remove</button>
                  </form>
                </li>
              );
            })}
          </ul>
          <div className="summary card">
            <span>
              Estimated total <strong>{formatMoney(estimate)}</strong>
            </span>
            <form action={startCheckout}>
              <button className="primary">Checkout</button>
            </form>
          </div>
        </>
      )}
    </>
  );
}
