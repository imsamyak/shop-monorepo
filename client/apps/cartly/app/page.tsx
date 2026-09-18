import Link from 'next/link';
import { api } from '@/lib/sdk';
import { formatMoney } from '@/lib/format';
import { mainImage } from '@/lib/images';

const SORTS = {
  'createdAt,desc': 'Newest',
  'price,asc': 'Price: low to high',
  'price,desc': 'Price: high to low',
} as const;

type Sort = keyof typeof SORTS;

const PAGE_SIZE = 12;

export default async function HomePage({ searchParams }: PageProps<'/'>) {
  const query = await searchParams;
  const sort: Sort = typeof query.sort === 'string' && query.sort in SORTS ? (query.sort as Sort) : 'createdAt,desc';
  const inStock = query.inStock === '1';
  const page = Math.max(0, Number(query.page) || 0);

  const result = await api().catalog.listCatalogProducts({
    page,
    size: PAGE_SIZE,
    sort,
    availability: inStock ? 'AVAILABLE' : undefined,
  });

  const href = (next: number) => {
    const params = new URLSearchParams({ sort, page: String(next) });
    if (inStock) params.set('inStock', '1');
    return `/?${params}`;
  };

  return (
    <>
      <form className="toolbar">
        <h1>All products</h1>
        <div className="row">
          <label className="check">
            <input type="checkbox" name="inStock" value="1" defaultChecked={inStock} />
            In stock only
          </label>
          <select name="sort" defaultValue={sort} aria-label="Sort">
            {Object.entries(SORTS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <button>Apply</button>
        </div>
      </form>

      {result.items.length === 0 ? (
        <p className="empty">No products yet.</p>
      ) : (
        <ul className="grid">
          {result.items.map((product) => (
            <li key={product.id}>
              <Link href={`/products/${product.id}`} className="tile">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={mainImage(product.images)} alt="" loading="lazy" />
                <span className="title">{product.title}</span>
                <span className="price">
                  {product.lowestSkuPrice !== null && product.lowestSkuPrice < product.price && 'from '}
                  {formatMoney(product.lowestSkuPrice ?? product.price)}
                </span>
                {product.availability === 'OUT_OF_STOCK' && <span className="badge out">Out of stock</span>}
              </Link>
            </li>
          ))}
        </ul>
      )}

      {result.totalPages > 1 && (
        <nav className="pager">
          {page > 0 ? <Link href={href(page - 1)}>← Previous</Link> : <span />}
          <span className="muted">
            Page {page + 1} of {result.totalPages}
          </span>
          {page + 1 < result.totalPages ? <Link href={href(page + 1)}>Next →</Link> : <span />}
        </nav>
      )}
    </>
  );
}
