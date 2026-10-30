import type { Metadata } from 'next';
import { HexagonDiagram, InfraDiagram, SystemDiagram } from '@/components/ArchDiagrams';

export const metadata: Metadata = {
  title: 'Architecture',
  description: 'How Cartly is built: system design, service design, patterns, security and deployment.',
};

const repository = 'https://github.com/imsamyak/shop-monorepo';

const facts = [
  ['7', 'Spring Boot microservices'],
  ['11', 'containers on one server'],
  ['1', 'command to deploy'],
  ['200', 'platform SDK tests'],
];

const flow = [
  ['Checkout form', "A Cartly server action reads the customer's JWT from the httpOnly cookie, which JavaScript in the page can never see, and calls the checkout service."],
  ['Checkout service', "Verifies the JWT itself (RS256, public keys from the identity service's JWKS), prices the lines from listing and reserves stock in inventory over REST, using a short-lived service token."],
  ['Outbox', 'The checkout state and its event (CheckoutDrafted, Committed or Cancelled) are written in the same database transaction. A relay then publishes the event to RabbitMQ, so no event is sent for a change that rolled back, and none is lost after one that committed.'],
  ['Inbox', 'The order service consumes the event through an inbox that discards duplicates, so a redelivered message is harmless, and builds the order and its status timeline.'],
  ['Read model', 'Inventory and listing events update the catalog service, which keeps a MongoDB read model for fast browsing. The storefront lists products from it, never from the write side.'],
];

const patterns = [
  ['Hexagonal architecture', 'Every service: adapter/in and adapter/out around the use cases; infra holds the adapters', 'Business rules do not know about HTTP, JPA or the broker. Kafka and RabbitMQ adapters both exist, so the transport swaps without touching a use case.'],
  ['Command and query use cases', 'CommandUseCase and QueryUseCase in the shared module, one class per use case', 'Controllers stay thin, and a read can never change state by accident.'],
  ['Transactional outbox', 'outbox module with an aspect and a relay', 'State and event commit together; no dual-write problem.'],
  ['Idempotent consumer (inbox)', 'inbox module with InboxDeduplicationAspect', 'At-least-once delivery from the broker becomes effectively-once processing.'],
  ['Aspect-oriented proxies', 'Outbox and inbox aspects, SecurityInterceptor for role annotations', 'Cross-cutting rules live in one place and are declared with annotations.'],
  ['Adapter', 'InventoryClientAdapter, ListingClientAdapter, ServiceTokenProvider in checkout', "A service calls another through a port; the HTTP details stay in one class."],
  ['Builder', "The SDK's useCase('Login').service('identity').post(...).build()", "Every endpoint is declared once and typed, and a test checks the list against the platform's api.json."],
  ['Factory and registry', 'createEcomSdk, OutboxEventFactory, DomainEventRegistry', 'Construction and event types are decided in one place.'],
  ['Facade', 'sdk.with(ctx) gives the storefront one object for all seven services', 'The app never builds a URL or attaches a token itself.'],
  ['Filter chain', 'A servlet filter answers /health; the security interceptor checks the JWT and roles', 'Each concern is a link in the request chain, added by auto-configuration.'],
  ['Optimistic locking', '@Version on the shared DomainEntity', 'Concurrent updates fail loudly instead of overwriting each other.'],
];

const security = [
  ['Tokens', 'RS256 JWTs issued by identity. Each service verifies them itself against the JWKS, so there is no gateway to trust or bypass. The browser holds only an httpOnly cookie.'],
  ['Roles', 'Annotation-based: an endpoint declares who may call it, and one interceptor enforces it.'],
  ['Between services', 'Short-lived service tokens, never a customer token passed along by accident.'],
  ['The chatbot', "Acts as the signed-in customer only. Adding to the cart takes a two-step confirmation, and text written by sellers is wrapped as untrusted so it cannot give the model instructions."],
  ['The network', 'Databases and services have no published ports. One port (80) reaches the storefront; secrets live in an owner-only .env on the server.'],
];

const quality = [
  ['Contract test', "The platform documents every endpoint in api.json, and a test fails if the SDK is missing one or disagrees on its name, method, path or sign-in rule."],
  ['Integration tests', 'Every service is tested over real HTTP against its database and broker setup.'],
  ['SDKs', 'A shared engine (@shop/sdk-core) with a platform SDK (200 tests) and a chatbot SDK, used by both the storefront and the seeding script.'],
  ['Health and ordering', 'Every service and database has a health check; Compose starts the event consumers first and waits for health before the next layer.'],
  ['Deployment as code', 'One command (pnpm deploy:dev up) sets up the server, builds each image one at a time and seeds the shop.'],
];

const stack = [
  ['Backend', 'Java 21, Spring Boot 4, Spring Data JPA, Maven multi-module, PostgreSQL, MongoDB, RabbitMQ'],
  ['Storefront', 'Next.js 16, React 19, Redux Toolkit and RTK Query, Tailwind CSS 4'],
  ['Assistant', 'Node, Express, Gemini, a retrieval index of shop policies, tool calls into the platform'],
  ['Delivery', 'Docker multi-stage images, Docker Compose, an EC2 server, pnpm workspace'],
];

export default function ArchitecturePage() {
  return (
    <div className="arch">
      <section className="arch-hero">
        <p className="eyebrow">Architecture</p>
        <h1>How Cartly is built</h1>
        <p className="lead">
          A shop made of seven microservices that talk through REST and RabbitMQ events, a storefront that never exposes a token to the browser, and an
          AI assistant that shops on the customer&apos;s behalf. This is the whole system, from the diagram down to the server it runs on.
        </p>
        <ul className="arch-facts">
          {facts.map(([value, label]) => (
            <li key={label}>
              <strong>{value}</strong>
              <span>{label}</span>
            </li>
          ))}
        </ul>
        <a href={repository} className="button primary" target="_blank" rel="noreferrer">
          Source on GitHub
        </a>
      </section>

      <section className="arch-section">
        <h2>1. High-level design</h2>
        <p className="muted">
          The browser only ever talks to Cartly. Cartly, acting as a backend for the frontend, calls the platform with the customer&apos;s token. Each
          service owns its data; services learn about each other&apos;s changes from events, not from shared tables.
        </p>
        <SystemDiagram />
        <ul className="arch-notes">
          <li>One database per service (identity_db, listing_db, inventory_db, checkout_db, order_db); only the catalog uses MongoDB, as a read model.</li>
          <li>Services verify JWTs themselves against the identity service&apos;s public keys, so no service has to trust another&apos;s say-so.</li>
          <li>Every service answers GET /health and serves its API under /api.</li>
        </ul>
      </section>

      <section className="arch-section">
        <h2>2. What happens when you place an order</h2>
        <ol className="arch-flow">
          {flow.map(([title, text]) => (
            <li key={title}>
              <strong>{title}</strong>
              <p>{text}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="arch-section">
        <h2>3. Low-level design of a service</h2>
        <p className="muted">
          Every service has the same shape. Dependencies point inward: adapters know the core, the core knows only its own ports.
        </p>
        <HexagonDiagram />
        <div className="arch-table-wrap">
          <table className="arch-table">
            <thead>
              <tr>
                <th>Pattern</th>
                <th>Where</th>
                <th>Why</th>
              </tr>
            </thead>
            <tbody>
              {patterns.map(([name, where, why]) => (
                <tr key={name}>
                  <th scope="row">{name}</th>
                  <td>{where}</td>
                  <td>{why}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="arch-section">
        <h2>4. Security</h2>
        <dl className="arch-list">
          {security.map(([term, text]) => (
            <div key={term}>
              <dt>{term}</dt>
              <dd>{text}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="arch-section">
        <h2>5. Infrastructure and deployment</h2>
        <p className="muted">
          What you are using right now runs on one small AWS server. A Node command-line tool sets the server up over ssh, copies the code, builds every image
          and starts the stack.
        </p>
        <InfraDiagram />
        <ul className="arch-notes">
          <li>Each service has its own multi-stage Dockerfile; Maven builds inside the image, and the runtime image carries only the JRE.</li>
          <li>The server has 4 GB of memory, so builds run one at a time, JVM heaps are capped, and 4 GB of swap absorbs the peaks.</li>
          <li>Development uses a hosted RabbitMQ and an S3 bucket, so the server itself holds only the databases.</li>
        </ul>
      </section>

      <section className="arch-section">
        <h2>6. Quality and delivery</h2>
        <dl className="arch-list">
          {quality.map(([term, text]) => (
            <div key={term}>
              <dt>{term}</dt>
              <dd>{text}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="arch-section">
        <h2>7. Stack</h2>
        <dl className="arch-list">
          {stack.map(([term, text]) => (
            <div key={term}>
              <dt>{term}</dt>
              <dd>{text}</dd>
            </div>
          ))}
        </dl>
      </section>
    </div>
  );
}
