import { formatDateTime } from '@/lib/format';
import { CheckIcon } from './icons';

/** The steps of an order's journey. The platform does not report shipping yet, so only the first is ever reached. */
const STEPS = ['Placed', 'Ready to Ship', 'In Transit', 'Out for Delivery', 'Delivered'] as const;

/** How far along an order is. Today every order is at the first step; later this can come from the platform. */
const REACHED = 1;

export function OrderTimeline({ placedAt }: { placedAt: string | null }) {
  return (
    <section className="card timeline-card" aria-label="Order progress">
      <h2>Order status</h2>
      <ol className="timeline">
        {STEPS.map((step, index) => {
          const done = index < REACHED;
          return (
            <li key={step} className={done ? 'done' : undefined} aria-current={index === REACHED - 1 ? 'step' : undefined}>
              <span className="dot" aria-hidden>
                {done && <CheckIcon size={14} />}
              </span>
              <span className="step-text">
                <strong>{step}</strong>
                {index === 0 && placedAt && <small>{formatDateTime(placedAt)}</small>}
              </span>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
