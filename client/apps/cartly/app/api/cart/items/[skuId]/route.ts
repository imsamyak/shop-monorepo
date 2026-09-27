import { forUser, toCartView } from '@/lib/api-route';
import { api } from '@/lib/sdk';

function skuOf(value: string) {
  const skuId = Number(value);
  return Number.isInteger(skuId) && skuId > 0 ? skuId : null;
}

/** Sets a line's quantity; the cart `version` the buyer last saw guards against a change from another tab. */
export async function PATCH(request: Request, ctx: RouteContext<'/api/cart/items/[skuId]'>) {
  const skuId = skuOf((await ctx.params).skuId);
  const input = (await request.json().catch(() => null)) as { quantity?: unknown; version?: unknown } | null;
  const quantity = Number(input?.quantity);
  const version = Number(input?.version);
  if (!skuId || !Number.isInteger(quantity) || quantity < 0 || quantity > 1000 || !Number.isInteger(version) || version < 0) {
    return Response.json({ error: 'That quantity is not valid.' }, { status: 400 });
  }
  return forUser(async (user) =>
    toCartView(
      quantity > 0
        ? await api().cart.setCartItem({ customerId: user.id, skuId, quantity, version })
        : await api().cart.removeCartItem({ customerId: user.id, skuId, version }),
    ),
  );
}

export async function DELETE(request: Request, ctx: RouteContext<'/api/cart/items/[skuId]'>) {
  const skuId = skuOf((await ctx.params).skuId);
  const version = Number(new URL(request.url).searchParams.get('version'));
  if (!skuId || !Number.isInteger(version) || version < 0) {
    return Response.json({ error: 'That item is not valid.' }, { status: 400 });
  }
  return forUser(async (user) => toCartView(await api().cart.removeCartItem({ customerId: user.id, skuId, version })));
}
