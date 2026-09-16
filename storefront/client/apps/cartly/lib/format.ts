const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

export const formatMoney = (amount: number) => money.format(amount);

export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('en-US', { dateStyle: 'medium', timeStyle: 'short' });
