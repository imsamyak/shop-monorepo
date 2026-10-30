import type { ReactNode } from 'react';

/** The architecture page's diagrams: inline SVG drawn with the shop's own colours (see .d-* in globals.css). */

type Tone = 'plain' | 'accent' | 'soft' | 'store';

function Defs({ id }: { id: string }) {
  return (
    <defs>
      <marker id={id} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
        <path d="M0 0 L10 5 L0 10 z" className="d-head" />
      </marker>
    </defs>
  );
}

function Box({ x, y, w, h, title, sub = [], tone = 'plain' }: { x: number; y: number; w: number; h: number; title: string; sub?: string[]; tone?: Tone }) {
  const lineHeight = 16;
  const first = y + h / 2 - (sub.length * lineHeight) / 2 + 5;
  return (
    <g className={`d-box d-${tone}`}>
      <rect x={x} y={y} width={w} height={h} rx={10} />
      <text x={x + w / 2} y={first} textAnchor="middle" className="d-title">
        {title}
      </text>
      {sub.map((line, index) => (
        <text key={line} x={x + w / 2} y={first + (index + 1) * lineHeight} textAnchor="middle" className="d-sub">
          {line}
        </text>
      ))}
    </g>
  );
}

function Group({ x, y, w, h, title, children }: { x: number; y: number; w: number; h: number; title: string; children?: ReactNode }) {
  return (
    <g>
      <rect x={x} y={y} width={w} height={h} rx={16} className="d-group" />
      <text x={x + 18} y={y + 24} className="d-group-title">
        {title}
      </text>
      {children}
    </g>
  );
}

function Arrow({ id, x1, y1, x2, y2, both, label, dashed }: { id: string; x1: number; y1: number; x2: number; y2: number; both?: boolean; label?: string; dashed?: boolean }) {
  return (
    <g>
      <line
        x1={x1}
        y1={y1}
        x2={x2}
        y2={y2}
        className={dashed ? 'd-line d-dashed' : 'd-line'}
        markerEnd={`url(#${id})`}
        markerStart={both ? `url(#${id})` : undefined}
      />
      {label && (
        <text x={(x1 + x2) / 2} y={(y1 + y2) / 2 - 7} textAnchor="middle" className="d-label">
          {label}
        </text>
      )}
    </g>
  );
}

function Figure({ label, viewBox, minWidth, children }: { label: string; viewBox: string; minWidth: number; children: ReactNode }) {
  return (
    <div className="diagram">
      <svg viewBox={viewBox} role="img" aria-label={label} style={{ minWidth }}>
        {children}
      </svg>
    </div>
  );
}

const services: [string, string][] = [
  ['identity', 'sign-up, login, JWT and JWKS'],
  ['listing', "sellers' products and SKUs"],
  ['inventory', 'stock and reservations'],
  ['checkout', 'checkout orchestration'],
  ['order', 'orders and status timeline'],
  ['catalog', 'read model for browsing'],
  ['asset', 'photo upload to S3'],
];

/** High-level design: who talks to whom. */
export function SystemDiagram() {
  const id = 'sys-arrow';
  return (
    <Figure label="System architecture: browser, Cartly, chatbot, seven services, databases and RabbitMQ" viewBox="0 0 960 510" minWidth={760}>
      <Defs id={id} />
      <Box x={10} y={190} w={130} h={60} title="Browser" sub={['React 19', 'RTK Query']} />
      <Arrow id={id} x1={140} y1={220} x2={200} y2={220} />
      <Box x={200} y={140} w={170} h={160} title="Cartly" sub={['Next.js 16', 'backend for frontend', 'server actions', 'httpOnly JWT cookie']} tone="accent" />
      <Arrow id={id} x1={285} y1={300} x2={285} y2={340} label="REST" />
      <Box x={200} y={340} w={170} h={80} title="Chatbot" sub={['Node, Express', 'RAG and Gemini']} />
      <Box x={10} y={350} w={130} h={60} title="Gemini API" sub={['the language model']} />
      <Arrow id={id} x1={200} y1={380} x2={140} y2={380} />

      <Group x={420} y={10} w={290} h={410} title="Platform: 7 Spring Boot services">
        {services.map(([name, what], index) => (
          <Box key={name} x={440} y={48 + index * 52} w={250} h={42} title={name} sub={[what]} tone="soft" />
        ))}
      </Group>
      <Arrow id={id} x1={370} y1={200} x2={420} y2={200} label="REST + JWT" />
      <Arrow id={id} x1={370} y1={385} x2={420} y2={385} label="REST + JWT" />

      <Box x={770} y={40} w={180} h={86} title="PostgreSQL 16" sub={['one database', 'per service']} tone="store" />
      <Box x={770} y={160} w={180} h={66} title="MongoDB" sub={['catalog read model']} tone="store" />
      <Box x={770} y={260} w={180} h={66} title="Amazon S3" sub={['product photos']} tone="store" />
      <Arrow id={id} x1={710} y1={83} x2={770} y2={83} />
      <Arrow id={id} x1={710} y1={193} x2={770} y2={193} />
      <Arrow id={id} x1={710} y1={293} x2={770} y2={293} />

      <Box x={420} y={450} w={290} h={50} title="RabbitMQ (CloudAMQP)" sub={['domain events, outbox to inbox']} tone="accent" />
      <Arrow id={id} x1={565} y1={420} x2={565} y2={450} both label="events" />
    </Figure>
  );
}

/** Low-level design of one service: hexagonal architecture. */
export function HexagonDiagram() {
  const id = 'hex-arrow';
  return (
    <Figure label="Hexagonal architecture of a service: inbound adapters, application core, ports and outbound adapters" viewBox="0 0 960 300" minWidth={760}>
      <Defs id={id} />
      <text x={105} y={30} textAnchor="middle" className="d-group-title">
        Inbound adapters
      </text>
      <Box x={10} y={50} w={190} h={56} title="REST controller" sub={['/api/..., one per resource']} />
      <Box x={10} y={120} w={190} h={56} title="Event consumer" sub={['inbox, de-duplicated']} />
      <Box x={10} y={190} w={190} h={56} title="Filters and interceptor" sub={['health, JWT, roles']} />
      <Arrow id={id} x1={200} y1={78} x2={280} y2={78} />
      <Arrow id={id} x1={200} y1={148} x2={280} y2={148} />
      <Arrow id={id} x1={200} y1={218} x2={280} y2={218} />

      <Group x={280} y={20} w={400} h={250} title="Application core: business rules">
        <Box x={305} y={62} w={180} h={70} title="Use cases" sub={['CommandUseCase', 'QueryUseCase']} tone="accent" />
        <Box x={305} y={170} w={180} h={70} title="Domain model" sub={['aggregates, invariants,', 'optimistic locking']} tone="accent" />
        <Box x={520} y={100} w={140} h={100} title="Ports" sub={['interfaces the core', 'needs: repositories,', 'publisher, clients']} tone="soft" />
        <Arrow id={id} x1={395} y1={132} x2={395} y2={170} />
        <Arrow id={id} x1={485} y1={110} x2={520} y2={130} />
      </Group>

      <text x={855} y={30} textAnchor="middle" className="d-group-title">
        Outbound adapters
      </text>
      <Box x={760} y={50} w={190} h={56} title="JPA repositories" sub={['Spring Data, PostgreSQL']} />
      <Box x={760} y={120} w={190} h={56} title="Outbox relay" sub={['RabbitMQ or Kafka']} />
      <Box x={760} y={190} w={190} h={56} title="HTTP and storage clients" sub={['services, S3, MongoDB']} />
      <Arrow id={id} x1={760} y1={78} x2={680} y2={78} label="implements" />
      <Arrow id={id} x1={760} y1={148} x2={680} y2={148} />
      <Arrow id={id} x1={760} y1={218} x2={680} y2={218} />
    </Figure>
  );
}

/** Where it runs. */
export function InfraDiagram() {
  const id = 'infra-arrow';
  const inside: [string, string][] = [
    ['7 Spring Boot services', 'Java 21, one multi-stage image each'],
    ['Cartly and the chatbot', 'Next.js standalone, Node 24'],
    ['PostgreSQL 16 and MongoDB 8', 'volumes, health-checked'],
  ];
  return (
    <Figure label="Deployment: deploy tool, an EC2 server running Docker Compose, and the hosted services it uses" viewBox="0 0 960 330" minWidth={760}>
      <Defs id={id} />
      <Box x={10} y={110} w={160} h={90} title="Deploy tool" sub={['pnpm deploy:dev', 'Node, ssh and tar']} tone="accent" />
      <Arrow id={id} x1={170} y1={155} x2={230} y2={155} label="ssh" />

      <Group x={230} y={20} w={430} h={290} title="AWS EC2, Mumbai: Docker Compose, 11 containers">
        {inside.map(([title, sub], index) => (
          <Box key={title} x={255} y={52 + index * 74} w={380} h={60} title={title} sub={[sub]} tone="soft" />
        ))}
        <text x={255} y={288} className="d-sub">
          One private network; only port 80 is published.
        </text>
      </Group>

      <Box x={760} y={30} w={190} h={60} title="Visitors" sub={['browser']} />
      <Arrow id={id} x1={760} y1={60} x2={660} y2={60} label="HTTP :80" />
      <Box x={760} y={120} w={190} h={60} title="CloudAMQP" sub={['hosted RabbitMQ']} tone="store" />
      <Arrow id={id} x1={660} y1={150} x2={760} y2={150} label="AMQPS" />
      <Box x={760} y={200} w={190} h={50} title="Amazon S3" sub={['ap-south-1']} tone="store" />
      <Arrow id={id} x1={660} y1={225} x2={760} y2={225} label="HTTPS" />
      <Box x={760} y={265} w={190} h={50} title="Gemini API" sub={['chatbot model']} tone="store" />
      <Arrow id={id} x1={660} y1={290} x2={760} y2={290} label="HTTPS" />
    </Figure>
  );
}
