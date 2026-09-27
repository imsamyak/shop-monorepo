import { ProductBrowser } from '@/components/ProductBrowser';
import { ReturnIcon, ShieldIcon, TruckIcon } from '@/components/icons';

export default function HomePage() {
  return (
    <>
      <section className="hero">
        <div>
          <p className="eyebrow">New arrivals every day</p>
          <h1>Find something you will love.</h1>
          <p className="lead">Fashion, gadgets, beauty and home, all in one cart.</p>
          <a href="#products" className="button primary">
            Shop now
          </a>
        </div>
        <ul className="hero-perks">
          <li>
            <TruckIcon /> Delivery in 3 to 5 days
          </li>
          <li>
            <ReturnIcon /> 30-day returns
          </li>
          <li>
            <ShieldIcon /> Secure checkout
          </li>
        </ul>
      </section>
      <ProductBrowser />
    </>
  );
}
