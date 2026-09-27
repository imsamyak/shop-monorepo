import Link from 'next/link';
import { CompassIcon } from '@/components/icons';
import { ErrorScreen } from '@/components/ErrorScreen';

export default function NotFound() {
  return (
    <ErrorScreen
      icon={<CompassIcon />}
      eyebrow="Error 404"
      title="We could not find that page."
      message="The link may be old, or the product may no longer be sold. Let us get you back to shopping."
    >
      <Link href="/" className="rounded-xl bg-accent px-6 py-3 font-bold text-white hover:bg-accent-hover hover:no-underline">
        Go to home
      </Link>
    </ErrorScreen>
  );
}
