import { forUser, loadCart } from '@/lib/api-route';

export const GET = () => forUser((user) => loadCart(user.id));
