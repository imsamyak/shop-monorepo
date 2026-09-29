import Link from 'next/link';
import type { SkuInfo } from '@/lib/catalog';
import { skuLabel } from '@/lib/catalog';
import { formatMoney } from '@/lib/format';
import { mainImage } from '@/lib/images';

interface Line {
  skuId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/** The priced lines of a checkout or an order, each with its picture. */
export function LineItems({ items, index }: { items: Line[]; index: Map<number, SkuInfo> }) {
  return (
    <ul className="cart">
      {items.map((item) => {
        const info = index.get(item.skuId);
        return (
          <li key={item.skuId} className="card cart-line">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={mainImage(info?.product.images)} alt="" />
            <div className="grow">
              {info ? <Link href={`/products/${info.product.id}`}>{info.product.title}</Link> : <span>Item {item.skuId}</span>}
              <div className="muted">{info ? `${skuLabel(info.sku)} · ` : ''}{formatMoney(item.unitPrice)} each</div>
              <div className="muted small">Qty {item.quantity}</div>
            </div>
            <strong className="line-total">{formatMoney(item.subtotal)}</strong>
          </li>
        );
      })}
    </ul>
  );
}

/** The totals beside the lines; the buttons for the page go inside. */
export function OrderSummary({
  title,
  items,
  total,
  children,
}: {
  title: string;
  items: Line[];
  total: number;
  children?: React.ReactNode;
}) {
  const units = items.reduce((sum, item) => sum + item.quantity, 0);
  return (
    <aside className="summary card">
      <h2>{title}</h2>
      <div className="sum-row">
        <span className="muted">Items ({units})</span>
        <span>{formatMoney(total)}</span>
      </div>
      <div className="sum-row total">
        <span>Total</span>
        <strong>{formatMoney(total)}</strong>
      </div>
      {children}
    </aside>
  );
}
