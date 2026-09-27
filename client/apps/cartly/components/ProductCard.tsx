import Link from 'next/link';
import { formatMoney } from '@/lib/format';
import type { ProductCard as Card } from '@/lib/types';

export function ProductCard({ product }: { product: Card }) {
  return (
    <Link href={`/products/${product.id}`} className={`tile${product.available ? '' : ' sold-out'}`}>
      <div className="tile-media">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={product.image} alt="" loading="lazy" />
        {!product.available && <span className="ribbon">Out of stock</span>}
      </div>
      <div className="tile-body">
        <span className="title">{product.title}</span>
        <span className="price">
          {product.from && <small>from </small>}
          {formatMoney(product.price)}
        </span>
      </div>
    </Link>
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="tile skeleton" aria-hidden>
      <div className="tile-media" />
      <div className="tile-body">
        <span className="line" />
        <span className="line short" />
      </div>
    </div>
  );
}
