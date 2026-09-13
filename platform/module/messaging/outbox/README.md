# Transactional Outbox Starter Module (`module/messaging/outbox`)

A production-ready, Spring Boot starter implementing the **Transactional Outbox Pattern**. It guarantees reliable event publishing from relational databases to message brokers (e.g., Kafka, RabbitMQ) with **at-least-once delivery semantics**, **zero distributed transactions (XA)**, **pessimistic row-level leasing**, **exponential backoff with jitter**, **dead-lettering**, and **automated data retention purging**.

---

## High-Level Architecture Diagram

```mermaid
flowchart TD

    %% -------------------------------------------------------------
    %% CLASS DEFINITIONS (Visual Themes)
    %% -------------------------------------------------------------
    classDef publicApi fill:#064e3b,stroke:#10b981,stroke-width:2px,color:#ffffff;
    classDef internalEngine fill:#1e293b,stroke:#38bdf8,stroke-width:2px,color:#ffffff;
    classDef dbStorage fill:#451a03,stroke:#f59e0b,stroke-width:2px,color:#ffffff;
    classDef brokerStorage fill:#701a75,stroke:#f472b6,stroke-width:2px,color:#ffffff;
    classDef triggerStyle fill:#3b0764,stroke:#c084fc,stroke-width:2px,color:#ffffff;

    %% -------------------------------------------------------------
    %% TOP ROW: LEGEND BAR (HORIZONTAL, NO BACKGROUND, NO TITLE)
    %% -------------------------------------------------------------
    subgraph LEGEND [" "]
        direction LR
        LEG_PUB["Public API (com.ecom.outbox)"]:::publicApi
        LEG_INT["Internal Engine (com.ecom.outbox.internal)"]:::internalEngine
        LEG_DB[("PostgreSQL Database")]:::dbStorage
        LEG_MB[("Message Broker")]:::brokerStorage
        LEG_TR["Scheduler / Async Trigger"]:::triggerStyle

        LEG_PUB ~~~ LEG_INT ~~~ LEG_DB ~~~ LEG_MB ~~~ LEG_TR
    end
    style LEGEND fill:none,stroke:none;

    %% -------------------------------------------------------------
    %% MIDDLE-LEFT: SCHEDULED SAFETY NET
    %% -------------------------------------------------------------
    subgraph SAFETY_NET ["Scheduled Safety Net"]
        direction TB
        TIMER_POLL["Timer Poller: 5s"]:::triggerStyle
        CLEANER_TASK["OutboxCleaner: 2 AM"]:::publicApi
    end

    %% -------------------------------------------------------------
    %% MIDDLE-CENTER: SUB-10MS NOTIFICATION & RELAY
    %% -------------------------------------------------------------
    subgraph NOTIFICATION_ZONE ["Sub-10ms Notification"]
        direction TB
        TRIGGER["OutboxEventTrigger"]:::publicApi
        VTHREAD["Async Trigger (Virtual Thread)"]:::internalEngine
        RELAY["OutboxRelay"]:::publicApi
        PROCESSOR["OutboxProcessor"]:::publicApi
    end

    %% -------------------------------------------------------------
    %% MIDDLE-RIGHT: DECOUPLED RELAY PIPELINE
    %% -------------------------------------------------------------
    subgraph BROKER_ZONE ["Decoupled Relay Pipeline"]
        direction TB
        PUB_SPI["OutboxPublisher"]:::publicApi
        HOST_PUB["Kafka / RabbitMQ Publisher"]:::internalEngine
        BROKER[("Message Broker")]:::brokerStorage
    end

    %% -------------------------------------------------------------
    %% BOTTOM: ATOMIC DB TRANSACTION
    %% -------------------------------------------------------------
    subgraph ATOMIC_TX ["Atomic DB Transaction"]
        direction LR
        CTRL["Controller"]:::internalEngine
        SVC["OutboxUseCase"]:::publicApi
        AOP["OutboxAspect"]:::internalEngine
        FACT["OutboxEventFactory"]:::publicApi
        
        subgraph DB_TABLES ["PostgreSQL Relational Database"]
            direction TB
            BIZ_DB[("Business Tables")]:::dbStorage
            OUTBOX_DB[("outbox_events Table")]:::dbStorage
        end
    end

    %% -------------------------------------------------------------
    %% FLOW ARROWS & INTERACTIONS
    %% -------------------------------------------------------------

    %% Top Row Legend alignment above the 3 architecture zones
    LEG_PUB ~~~ SAFETY_NET
    LEG_DB ~~~ NOTIFICATION_ZONE
    LEG_TR ~~~ BROKER_ZONE

    %% Inside Atomic DB Transaction
    CTRL --> SVC
    SVC --> AOP
    AOP -->|"save entity"| BIZ_DB
    AOP -->|"via OutboxRepository"| OUTBOX_DB
    AOP -.-> FACT

    %% Commit event to Trigger
    OUTBOX_DB -. "OutboxCreatedEvent (AFTER_COMMIT)" .-> TRIGGER

    %% Notification zone flow
    TRIGGER --> VTHREAD
    VTHREAD --> RELAY
    TIMER_POLL --> RELAY
    RELAY --> PROCESSOR

    %% 3-Step Execution Loop
    PROCESSOR -->|"1. Short Lock Lease"| OUTBOX_DB
    PROCESSOR -->|"2. Publish Network I/O"| PUB_SPI
    PUB_SPI --> HOST_PUB
    HOST_PUB --> BROKER
    PROCESSOR -->|"3. Mark PROCESSED / DLQ"| OUTBOX_DB

    %% Cleaner purge
    CLEANER_TASK -->|"Delete old PROCESSED"| OUTBOX_DB
```

---

## Detailed File-by-File Breakdown: Why, What, & How (With Zoomed Detail Flows)

Every file in this module has a single, well-defined responsibility.

---

<a id="outbox-use-case"></a><a id="domain-event"></a>
### 1. `OutboxUseCase<C, R>` (`com.ecom.outbox`) & `DomainEvent` (`com.ecom.shared.messaging`)

```mermaid
flowchart LR
    CTRL["Client / Controller<br/><code>POST /api/v1/products</code>"] -->|"execute(command)"| UC["<b>OutboxUseCase&lt;C, R&gt;</b><br/><code>execute(CreateProductCommand)</code>"]
    UC -->|"1. mutate domain"| BIZ["Product Entity<br/>saved to products table"]
    UC -->|"2. buildEvent(cmd, res)"| DE["<b>DomainEvent</b><br/><code>record Product.Created(id, title)</code>"]
    DE -->|"returned to"| ASP["OutboxAspect Interceptor"]
```

* **Why**: To provide a clean, declarative programming model for application developers. Business use cases need to emit events without manually writing boilerplate SQL or managing outbox entity lifecycles.
* **What**: 
  * `OutboxUseCase<C, R>`: An interface extending `com.ecom.shared.usecase.CommandUseCase<C, R>`, adding the `DomainEvent buildEvent(C cmd, CommandResult<R> result)` contract.
  * `DomainEvent` (`com.ecom.shared.messaging`): Shared contract defining `String aggregateId()`. Aggregate domain events are defined as nested Java records inside an aggregate interface (e.g., `Product.Created(String id, String title) implements DomainEvent`).
* **How**: Developers implement `OutboxUseCase` on their command use case classes. If `buildEvent(...)` returns non-null, `OutboxAspect` automatically serializes and commits the event into `outbox_events` within the use case transaction. If it returns `null`, outbox recording is skipped.

---

<a id="outbox-aspect"></a><a id="outbox-created-event"></a>
### 2. `OutboxAspect` & `OutboxCreatedEvent` (`com.ecom.outbox.internal.aspect`)

```mermaid
flowchart LR
    UC["OutboxUseCase.execute()"] -. "@Around" .-> ASP["OutboxAspect"]
    ASP --> TX["TransactionTemplate"]
    TX --> BIZ["Business Logic"]
    ASP --> FACT["OutboxEventFactory"]
    FACT --> ENT["OutboxEntity"]
    ENT --> REPO["OutboxRepository"]
    REPO -. "AFTER_COMMIT" .-> EVT["OutboxCreatedEvent"]
    EVT --> TRIG["OutboxEventTrigger"]
```

* **Why**: To enforce transaction atomicity. An outbox event must commit if and only if the underlying business mutation commits. Writing to the event table manually risks forgotten saves or partial failures.
* **What**: 
  * `OutboxAspect`: AspectJ around advice (`@Aspect`) intercepting `OutboxUseCase+.execute(..)`.
  * `OutboxCreatedEvent`: Internal Spring `ApplicationEvent` holding the `UUID outboxId` of the committed record.
* **How**: 
  1. Wraps invocation in `TransactionTemplate`.
  2. Executes business code: `joinPoint.proceed()`.
  3. Calls `useCase.buildEvent(command, result)`.
  4. Serializes the event via `OutboxEventFactory`.
  5. Saves `OutboxEntity` with status `PENDING`.
  6. Fires `OutboxCreatedEvent` via Spring's `ApplicationEventPublisher`.

---

<a id="outbox-event-factory"></a><a id="default-outbox-event-factory"></a>
### 3. `OutboxEventFactory` & `DefaultOutboxEventFactory`

```mermaid
flowchart LR
    DE["<b>DomainEvent</b><br/>Record: <code>Product.Created</code>"] --> FACT["<b>DefaultOutboxEventFactory</b><br/>Extracts aggregate &amp; serializes JSON"]
    FACT -->|"wraps into"| ENV["<b>JSON Envelope</b><br/>aggregate, action, data"]
    ENV -->|"produces"| OUT["<b>OutboxEvent</b> (Embeddable)<br/>aggregateType, aggregateId, payload"]
```

* **Why**: To safely validate and transform strongly-typed Java objects into standardized, size-capped JSON envelopes before persistence.
* **What**: 
  * `OutboxEventFactory` (`com.ecom.outbox`): Public factory contract.
  * `DefaultOutboxEventFactory` (`internal.aspect`): Default implementation.
* **How**:
  1. Validates that the event is a record nested inside an aggregate interface.
  2. Extracts `aggregateType` (declaring class simple name) and `action` (record class simple name).
  3. Enforces non-blank `aggregateId`.
  4. Wraps in `Envelope(aggregate, action, data)` and serializes using Jackson `ObjectMapper`.
  5. Verifies payload does not exceed `outbox.max-payload-length` (default: 64 KB).

---

<a id="outbox-event-trigger"></a><a id="default-outbox-event-trigger"></a>
### 4. `OutboxEventTrigger` & `DefaultOutboxEventTrigger`

```mermaid
flowchart LR
    COMMIT[("DB Transaction Commits")] -. "fires" .-> EVT["<b>OutboxCreatedEvent</b><br/>UUID outboxId"]
    EVT --> TRIG["<b>DefaultOutboxEventTrigger</b><br/><code>@TransactionalEventListener(phase = AFTER_COMMIT)</code>"]
    TRIG -->|"submits task"| EXEC["<b>SimpleAsyncTaskExecutor</b><br/>Virtual Thread Pool: outbox-trigger-"]
    EXEC -->|"non-blocking async dispatch"| RELAY["<b>OutboxRelay.processBatch()</b><br/><i>Sub-10ms response time</i>"]
```

* **Why**: To achieve sub-millisecond, near real-time event dispatching without waiting for the background polling tick.
* **What**: 
  * `OutboxEventTrigger` (`com.ecom.outbox`): Public trigger interface.
  * `DefaultOutboxEventTrigger` (`internal.relay`): Default implementation.
* **How**: Listens to `OutboxCreatedEvent` with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. Dispatches `relay::processBatch` to `SimpleAsyncTaskExecutor` configured with Java Virtual Threads (`setVirtualThreads(true)`).

---

<a id="outbox-relay"></a><a id="default-outbox-relay"></a>
### 5. `OutboxRelay` & `DefaultOutboxRelay`

```mermaid
flowchart LR
    TRIG["Async Trigger<br/>(&lt;10ms)"] --> RELAY["DefaultOutboxRelay"]
    POLL["Timer Poller<br/>(5s)"] --> RELAY
    RELAY --> GUARD{"isProcessing?"}
    GUARD -- "Running" --> SKIP["Skip (Return 0)"]
    GUARD -- "Lock Acquired" --> PROC["DefaultOutboxProcessor<br/>processBatch()"]
    PROC -->|"processed &gt; 0"| CLEAN["DefaultOutboxCleaner<br/>cleanup()"]
    PROC --> FINALLY["finally:<br/>isProcessing.set(false)"]
```

* **Why**: To coordinate execution between real-time event triggers and timer pollers while preventing concurrent execution collisions on the same node.
* **What**: 
  * `OutboxRelay` (`com.ecom.outbox`): Public orchestrator contract.
  * `DefaultOutboxRelay` (`internal.relay`): Default implementation with `AtomicBoolean isProcessing`.
* **How**: Uses `compareAndSet(false, true)` to ensure that if a batch is already in progress, overlapping trigger requests exit immediately. Delegates batch processing to `OutboxProcessor` and releases the guard in a `finally` block.

---

<a id="outbox-processor"></a><a id="default-outbox-processor"></a>
### 6. `OutboxProcessor` & `DefaultOutboxProcessor`

```mermaid
flowchart LR
    START["processBatch()"] --> REC["recoverStaleLeases()"]
    REC --> CLAIM["lockPendingBatch()"]
    CLAIM --> PUB["OutboxPublisher.publish()"]
    PUB -- "Success" --> OK["markProcessed()<br/>PROCESSED"]
    PUB -- "Exception" --> FAIL{"retries &gt;= max?"}
    FAIL -- "No" --> RETRY["ExponentialRetryBackoff<br/>PENDING"]
    FAIL -- "Yes" --> DLQ["DEAD_LETTER"]
```

* **Why**: To encapsulate the core business logic of outbox processing: leasing, broker dispatching, retries, and lease recovery.
* **What**: 
  * `OutboxProcessor` (`com.ecom.outbox`): Public processing contract (`processBatch()`, `recoverStaleLeases()`, `replayDeadLetterEvents()`).
  * `DefaultOutboxProcessor` (`internal.relay`): Default processing engine.
* **How**:
  1. Recovers expired in-flight leases left by crashed nodes.
  2. Runs a `do-while` loop claiming pending records with pessimistic write locks (`SELECT ... FOR UPDATE`).
  3. Dispatches via `OutboxPublisher`.
  4. On success: marks `PROCESSED`. On error: schedules retry via `RetryBackoffStrategy` or transitions to `DEAD_LETTER`.

---

<a id="outbox-publisher"></a><a id="outbox-message"></a><a id="default-outbox-message"></a>
### 7. `OutboxPublisher` & `OutboxMessage` (`com.ecom.outbox`) / `DefaultOutboxMessage` (`internal.relay`)

```mermaid
flowchart LR
    PROC["DefaultOutboxProcessor"] -->|"constructs"| MSG["<b>DefaultOutboxMessage</b> (Record)<br/><i>implements OutboxMessage</i><br/>UUID id<br/>String aggregateType<br/>String aggregateId<br/>String payload<br/>LocalDateTime createdAt"]
    MSG -->|"passed as OutboxMessage"| SPI["<b>OutboxPublisher (SPI)</b><br/><code>void publish(OutboxMessage)</code>"]
    SPI -. "implemented by host app" .-> KAFKA["<b>KafkaOutboxPublisher</b><br/><code>kafkaTemplate.send(msg.aggregateType(), msg.aggregateId(), msg.payload())</code>"]
    KAFKA --> BROKER[("<b>Kafka Broker</b><br/>Partitioned by aggregateId")]
```

* **Why**: To decouple the outbox module from any concrete message transport (Kafka, RabbitMQ, SQS, Google PubSub), while strictly guaranteeing that root API contains **interfaces only** and internal packages encapsulate concrete implementations.
* **What**: 
  * `OutboxPublisher` (`com.ecom.outbox`): Functional interface SPI defining `void publish(OutboxMessage message) throws Exception`.
  * `OutboxMessage` (`com.ecom.outbox`): Public pure interface defining `id()`, `aggregateType()`, `aggregateId()`, `payload()`, and `createdAt()`.
  * `DefaultOutboxMessage` (`com.ecom.outbox.internal.relay`): Immutable record implementation fulfilling the `OutboxMessage` contract.
* **How**: Host applications implement `OutboxPublisher` consuming the `OutboxMessage` interface. `DefaultOutboxProcessor` instantiates `DefaultOutboxMessage` internally and passes it across the SPI boundary. If no publisher bean is registered, events remain safely `PENDING`.

---

<a id="retry-backoff-strategy"></a><a id="exponential-retry-backoff"></a>
### 8. `RetryBackoffStrategy` & `ExponentialRetryBackoff`

```mermaid
flowchart LR
    FAIL["Publish Failure<br/>attempt: retryCount"] --> STRAT["<b>ExponentialRetryBackoff</b><br/><code>calculateNextRetry(retryCount)</code>"]
    STRAT --> CALC["<b>Backoff Formula</b><br/>1. exponential = baseIntervalMs * 2^retryCount<br/>2. capped = min(exponential, maxIntervalMs)<br/>3. jitter = capped * +/-20% Full Jitter<br/>4. delay = capped + random(jitter)"]
    CALC --> NEXT["<b>LocalDateTime</b><br/>now() + delay<br/>Assigned to <code>entity.nextRetryAt</code>"]
```

* **Why**: To prevent cascading broker overloads (thundering herd problem) during downstream outages with randomized backoff.
* **What**: 
  * `RetryBackoffStrategy` (`com.ecom.outbox`): Public strategy interface defining `LocalDateTime calculateNextRetry(int retryCount)`.
  * `ExponentialRetryBackoff` (`internal.relay`): Default implementation.
* **How**: Computes exponential delay capped at `outbox.retry.max-interval-ms` (default: 5 min) and adds $\pm 20\%$ randomized full jitter.

---

<a id="outbox-cleaner"></a><a id="default-outbox-cleaner"></a>
### 9. `OutboxCleaner` & `DefaultOutboxCleaner`

```mermaid
flowchart LR
    CRON["<b>Nightly Cron Task</b><br/><code>0 0 2 * * ?</code> (2:00 AM)"] --> CLEAN["<b>DefaultOutboxCleaner.cleanup()</b>"]
    CLEAN --> CALC["cutoff = now() - retentionDays<br/><i>(default: 7 days)</i>"]
    CALC --> QUERY["<b>OutboxRepository.deleteProcessedBefore</b><br/><code>DELETE FROM outbox_events WHERE status = 'PROCESSED' AND processed_at &lt; cutoff</code>"]
    QUERY --> DB[("<b>outbox_events Table</b><br/>Old processed rows purged")]
```

* **Why**: To prevent unbounded table growth in high-throughput applications and maintain fast index performance.
* **What**: 
  * `OutboxCleaner` (`com.ecom.outbox`): Public cleaner contract (`void cleanup()`).
  * `DefaultOutboxCleaner` (`internal.relay`): Default implementation.
* **How**: Scheduled via cron expression (default: 2:00 AM daily). Bulk-deletes `PROCESSED` rows older than `outbox.cleaner.retention-days` (default: 7 days).

---

<a id="outbox-persistence"></a><a id="outbox-entity"></a><a id="outbox-event"></a><a id="outbox-status"></a><a id="outbox-repository"></a>
### 10. `OutboxEntity`, `OutboxEvent`, `OutboxStatus`, & `OutboxRepository` (`internal.persistence`)

```mermaid
flowchart LR
    REPO["OutboxRepository"] -->|"manages"| ENT["OutboxEntity<br/>Table: outbox_events"]
    ENT -->|"embeds @Embedded"| EVT["OutboxEvent<br/>aggregateType, aggregateId, payload"]
    ENT -->|"status enum"| ST["OutboxStatus<br/>PENDING | IN_FLIGHT | PROCESSED | DEAD_LETTER"]
```

* **Why**: To model the relational database storage schema and provide optimized queries for pessimistic locking and bulk maintenance.
* **What**: 
  * `OutboxEntity`: JPA entity mapped to `outbox_events`.
  * `OutboxEvent`: `@Embeddable` Value Object storing payload and aggregate IDs.
  * `OutboxStatus`: Enum representing the lifecycle states (`PENDING`, `IN_FLIGHT`, `PROCESSED`, `DEAD_LETTER`).
  * `OutboxRepository`: Spring Data repository with `lockPendingBatch`, `releaseStaleInFlight`, `deleteProcessedBefore`, and `replayDeadLetter`.
* **How**: Configured with composite database indexes on `(status, created_at)` and `(status, next_retry_at, locked_until)` for zero-overhead polling.

---

<a id="outbox-configuration"></a><a id="outbox-auto-configuration"></a><a id="outbox-relay-configuration"></a><a id="outbox-properties"></a>
### 11. Configuration: `OutboxAutoConfiguration`, `OutboxRelayConfiguration`, & `OutboxProperties` (`internal.config`)

```mermaid
flowchart LR
    PROP["OutboxProperties<br/>outbox.*"] --> AUTO["OutboxAutoConfiguration"]
    PROP --> RELAY_CONF["OutboxRelayConfiguration"]
    AUTO --> APP["Spring Application Context"]
    RELAY_CONF --> APP
```

* **Why**: To provide zero-configuration Spring Boot starter ergonomics with conditional bean overrides and type-safe properties.
* **What**: 
  * `OutboxProperties`: Immutable hierarchical configuration record bound to `outbox.*`.
  * `OutboxAutoConfiguration`: Registered in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  * `OutboxRelayConfiguration`: Conditional slice for relay, thread pools, and programmatic `SchedulingConfigurer`.
* **How**: Automatically wires all default beans unless custom beans are supplied (`@ConditionalOnMissingBean`).

---

## Configuration Reference (`application.yml`)

```yaml
outbox:
  max-payload-length: 65536        # Max characters allowed in JSON payload (64 KB)
  poll-interval-ms: 5000           # Background polling interval in milliseconds
  relay:
    enabled: true                  # Enable or disable background relay worker
    batch-size: 100                # Number of events to claim per batch
    lease-seconds: 30              # Lock duration for in-flight claimed events
  cleaner:
    retention-days: 7              # Days to retain processed events before purge
    cron: "0 0 2 * * ?"            # Purge schedule (2:00 AM daily)
  retry:
    max-retries: 5                 # Max failures before moving to DEAD_LETTER
    base-interval-ms: 1000         # Initial exponential backoff delay (1s)
    max-interval-ms: 300000        # Maximum backoff cap (5 minutes)
```
