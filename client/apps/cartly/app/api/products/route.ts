import { forAnyone, toCard } from '@/lib/api-route';
import { api } from '@/lib/sdk';
import { SORTS, type ProductPage, type Sort } from '@/lib/types';

const MAX_SEARCH_RESULTS = 48;

function number(value: string | null, fallback: number, min: number, max: number) {
  const parsed = Number(value);
  return Number.isInteger(parsed) ? Math.min(max, Math.max(min, parsed)) : fallback;
}

/** The home page's products. A search looks titles up in the listing, then shows them as the catalog does. */
export async function GET(request: Request) {
  const params = new URL(request.url).searchParams;
  const sortParam = params.get('sort') ?? '';
  const sort: Sort = sortParam in SORTS ? (sortParam as Sort) : 'createdAt,desc';
  const page = number(params.get('page'), 0, 0, 10_000);
  const size = number(params.get('size'), 12, 1, 48);
  const inStock = params.get('inStock') === '1';
  const search = (params.get('search') ?? '').trim().slice(0, 100);

  return forAnyone(async (): Promise<ProductPage> => {
    if (!search) {
      const result = await api().catalog.listCatalogProducts({
        page,
        size,
        sort,
        availability: inStock ? 'AVAILABLE' : undefined,
      });
      return {
        items: result.items.map(toCard),
        page: result.page,
        totalPages: result.totalPages,
        totalElements: result.totalElements,
      };
    }

    const matches = await api().products.listProducts({ title: search, size: MAX_SEARCH_RESULTS });
    const found = (
      await Promise.all(matches.items.map((match) => api().catalog.getCatalogProduct({ id: match.id }).catch(() => null)))
    )
      .filter((product) => product !== null)
      .map(toCard)
      .filter((card) => !inStock || card.available);
    if (sort === 'price,asc') found.sort((a, b) => a.price - b.price);
    if (sort === 'price,desc') found.sort((a, b) => b.price - a.price);

    return {
      items: found.slice(page * size, page * size + size),
      page,
      totalPages: Math.ceil(found.length / size),
      totalElements: found.length,
    };
  });
}
