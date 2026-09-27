/** A whole-page message for something that went wrong: a big mark, a plain sentence and what to do next. */
export function ErrorScreen({
  icon,
  eyebrow,
  title,
  message,
  reference,
  children,
}: {
  icon: React.ReactNode;
  eyebrow: string;
  title: string;
  message: string;
  /** An id the shop can look up in its logs. */
  reference?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto flex min-h-[calc(100vh-240px)] max-w-xl flex-col items-center justify-center gap-5 py-12 text-center">
      <div className="grid size-28 place-items-center rounded-full bg-accent-soft text-accent shadow-card ring-8 ring-accent-soft/50">
        {icon}
      </div>
      <p className="text-sm font-bold tracking-[0.14em] text-muted uppercase">{eyebrow}</p>
      <h1 className="m-0 text-3xl font-extrabold tracking-tight sm:text-4xl">{title}</h1>
      <p className="max-w-md text-lg text-muted">{message}</p>
      <div className="mt-2 flex flex-wrap justify-center gap-3">{children}</div>
      {reference && <p className="text-xs text-muted">Reference: {reference}</p>}
    </div>
  );
}
