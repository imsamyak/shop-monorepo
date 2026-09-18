import Link from 'next/link';
import type { SkuInfo } from '@/lib/catalog';
import { skuLabel } from '@/lib/catalog';
import { formatMoney } from '@/lib/format';

interface Line {
  skuId: number;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

/** The priced lines of a checkout or an order, with the total underneath. */
export function LineItems({ items, index, total }: { items: Line[]; index: Map<number, SkuInfo>; total: number }) {
  return (
    <table className="lines">
      <thead>
        <tr>
          <th>Item</th>
          <th className="num">Price</th>
          <th className="num">Qty</th>
          <th className="num">Subtotal</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const info = index.get(item.skuId);
          return (
            <tr key={item.skuId}>
              <td>
                {info ? (
                  <Link href={`/products/${info.product.id}`}>
                    {info.product.title} <span className="muted">· {skuLabel(info.sku)}</span>
                  </Link>
                ) : (
                  `SKU #${item.skuId}`
                )}
              </td>
              <td className="num">{formatMoney(item.unitPrice)}</td>
              <td className="num">{item.quantity}</td>
              <td className="num">{formatMoney(item.subtotal)}</td>
            </tr>
          );
        })}
      </tbody>
      <tfoot>
        <tr>
          <td colSpan={3}>Total</td>
          <td className="num">{formatMoney(total)}</td>
        </tr>
      </tfoot>
    </table>
  );
}
