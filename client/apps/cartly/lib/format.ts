const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

/** Prices are whole rupees with the ₹ symbol: 1499 reads ₹1,499, and 1234567 reads ₹12,34,567. */
export const formatMoney = (amount: number) => rupees.format(Math.round(amount));

/** The order number customers see: `OD` and the first ten characters of the id. Only the UI knows this form. */
export const orderNumber = (orderId: string) => `OD${orderId.replace(/-/g, '').slice(0, 10).toUpperCase()}`;

export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
