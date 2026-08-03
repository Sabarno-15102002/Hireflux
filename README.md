# HireFlux

A backend hiring platform that matches candidates to jobs using AI-generated resume embeddings, a weighted multi-strategy scoring engine, and an adaptive skill-relationship graph — built as an event-driven, horizontally scalable Spring Boot service.

Resume parsing runs asynchronously off the request path via Kafka, job search is served from Elasticsearch, hot reads are cached in Redis, and the whole system is instrumented end-to-end with Micrometer/Prometheus metrics and structured logs.

---

## Table of Contents

- [Why this project exists](#why-this-project-exists)
- [Architecture](#architecture)
- [Core design decisions](#core-design-decisions)
- [Tech stack](#tech-stack)
- [API surface](#api-surface)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Testing](#testing)
- [Observability](#observability)
- [Known limitations / roadmap](#known-limitations--roadmap)
- [License](#license)

---

## Why this project exists

Most CRUD-heavy hiring platforms treat "matching" as a SQL `WHERE` clause on keywords. HireFlux instead treats matching as a small, extensible scoring problem: resumes and jobs are embedded into vector space, skills are modeled as a weighted co-occurrence graph that gets smarter over time, and the final score is a pluggable weighted sum of independent strategies. The system is built the way a matching/recommendation service at a real product company would be — decoupled ingestion, idempotent processing, retryable async pipelines, and a strategy layer that new signals can be added to without touching existing code.

## Architecture

```
                         ┌─────────────────────┐
                         │   Client / Frontend  │
                         └──────────┬───────────┘
                                    │ JWT (stateless) / OAuth2
                         ┌──────────▼───────────┐
                         │   Spring Security     │
                         │  Filter Chain (JWT)   │
                         └──────────┬───────────┘
                                    │
        ┌───────────────┬──────────┼──────────────┬─────────────────┐
        ▼               ▼          ▼               ▼                 ▼
   AuthController  JobController  ResumeController  ApplicationController AdminController
        │               │          │               │                 │
        ▼               ▼          ▼               ▼                 ▼
   UserService     JobService  ResumeService  JobApplicationService  AdminService
                       │          │               │
                       ▼          ▼               ▼
                 ElasticSearch  S3 (raw file)  JobMatchingEngine
                  (job search)       │           (strategy pattern)
                                     ▼               │
                             ResumeEventProducer      ├─ EmbeddingMatchingStrategy (0.5)
                                     │                ├─ SkillMatchingStrategy      (0.2)
                                     ▼                ├─ ExperienceMatchingStrategy (0.2)
                              ┌────────────┐          └─ LocationMatchingStrategy   (0.1)
                              │   Kafka    │
                              │  (topic:   │
                              │  resume-   │
                              │  uploaded) │
                              └─────┬──────┘
                                    ▼
                          ResumeEventConsumer
                     (@RetryableTopic, exp. backoff, DLT)
                                    │
                    ┌───────────────┼────────────────┐
                    ▼               ▼                ▼
              Apache Tika      OpenAI API       EmbeddingService
             (text extract)   (structured        (vector embedding)
                               resume parse)
                    │               │                │
                    └───────────────┴────────────────┘
                                    ▼
                        Resume (PROCESSED, embedding, parsed JSON)
                                    ▼
                          SkillGraphService
                (in-memory cache + async batched flush to Postgres,
                 optimistic-lock retry, co-occurrence weighting)

   Cross-cutting: Redis (@Cacheable/@CacheEvict), Micrometer → Prometheus → Grafana,
                  structured event=... logs, Bucket4j rate limiting, Kibana over ES.
```

### Resume processing pipeline (the core async flow)

1. **Upload** — `POST /api/resume/presign` issues an S3 pre-signed URL; the client uploads directly to S3, then `POST /api/resume/upload` registers the resume as `UPLOADED`. Lookup by `fileKey` makes this endpoint idempotent against retried client uploads.
2. **Publish** — `ResumeEventProducer` emits a `ResumeUploadedEvent` to Kafka; the request returns immediately (parsing is never on the request path).
3. **Consume** — `ResumeEventConsumer` is annotated with `@RetryableTopic` (3 attempts, exponential backoff 2s→10s) and routes to a Dead Letter Topic on exhaustion. `NonRetryableProcessingException` (e.g. corrupt file) is explicitly excluded from retry — no point retrying a file that will never parse.
4. **Process** — `ResumeServiceImpl.processResume` runs a strict state machine (`UPLOADED → PROCESSING → PROCESSED | FAILED`), is idempotent (re-delivered messages against an already-`PROCESSED` resume are a no-op), and distinguishes failure modes precisely: `SdkClientException` (transient S3 issue → retryable) vs `InvalidFormatException` (corrupt upload → non-retryable) vs anything else. Every terminal state and transition is logged as a structured `event=...` key-value line for log-based alerting/search, and success/failure/retry/DLQ counts are pushed to Micrometer.
5. **Extract → Parse → Embed** — Apache Tika extracts raw text, `OpenAIService` returns structured resume data (skills, experience, education), and `EmbeddingService` generates a vector embedding that's persisted alongside the parsed JSON.

### Job matching engine

A `Strategy` pattern where Spring auto-collects every `MatchingStrategy` bean into `List<MatchingStrategy>`, so adding a new signal (e.g. salary-fit, culture-fit) means writing one new `@Component` — the engine and existing strategies are never touched:

```java
double score = strategies.stream()
    .mapToDouble(s -> s.calculate(context) * s.weight())
    .sum();
```

| Strategy | Weight | Signal |
|---|---|---|
| `EmbeddingMatchingStrategy` | 0.5 | Cosine similarity between resume and job embedding vectors |
| `SkillMatchingStrategy` | 0.2 | Best pairwise skill similarity via the skill graph |
| `ExperienceMatchingStrategy` | 0.2 | Linear penalty if under min experience, soft cap if over max |
| `LocationMatchingStrategy` | 0.1 | Exact-match bonus, partial credit otherwise |

Weights are fixed at compile time and sum to 1.0 by convention (not enforced at runtime — a validation/registration step is a natural next improvement).

### Skill graph — adaptive similarity without a static taxonomy

Rather than hardcoding a skills taxonomy ("Java" is related to "Spring", "Java" is related to "Kotlin"), `SkillGraphServiceImpl` **learns** skill relationships from co-occurrence across parsed resumes:

- Every pair of skills seen together on a resume has its edge weight bumped: `weight = min(log(1 + coOccurrence) / 5, 1.0)` — a logarithmic curve so early co-occurrences move the needle a lot and later ones diminish, capped at 1.0.
- Writes are absorbed into an in-memory `ConcurrentHashMap` immediately (so `getSimilarity()` reads are never blocked on the DB) and a dirty-key set is flushed to Postgres on a fixed 5-second schedule, batched, in a synchronized flush section.
- Concurrent flush conflicts are handled with optimistic-lock retry: on `ObjectOptimisticLockingFailureException`, the latest row is reloaded, the increment is reapplied, and the save is retried up to 3 times before surfacing a `ConflictException`.
- State is preloaded into memory on `@PostConstruct` and force-flushed on `@PreDestroy` so no learned signal is lost on a graceful shutdown.

## Core design decisions

- **Async off the request path, not async everywhere.** Resume parsing (slow, external-API-dependent, retryable) goes through Kafka. Job search and application flows stay synchronous because they need immediate, consistent responses.
- **Idempotency as a first-class concern**, not an afterthought: resume upload dedupes on `fileKey`, resume processing short-circuits on an already-`PROCESSED` status. Both matter because Kafka guarantees at-least-once delivery.
- **Exception taxonomy drives retry behavior.** `RetryableProcessingException` / `NonRetryableProcessingException` aren't just naming — they're read by `@RetryableTopic`'s `exclude` list to decide whether Kafka should retry or dead-letter immediately.
- **Strategy pattern for matching** keeps the scoring engine open for extension (new signals) and closed for modification (existing strategies/engine untouched), and keeps each signal independently unit-testable.
- **Stateless JWT auth** with `SessionCreationPolicy.STATELESS` so the service horizontally scales without sticky sessions or shared session storage.

## Tech stack

| Layer | Choice |
|---|---|
| Language / Framework | Java, Spring Boot (Web, Security, Data JPA, Validation, Cache, WebFlux client) |
| Datastore | PostgreSQL (system of record) |
| Search | Elasticsearch (job search/discovery) |
| Cache | Redis (`@Cacheable`/`@CacheEvict` on hot reads) |
| Messaging | Apache Kafka (`spring-kafka`, retryable topics + DLT) |
| Object storage | AWS S3 (resume files, pre-signed upload/download) |
| AI | OpenAI API (resume parsing), custom embedding generation |
| Auth | JWT (`jjwt`) + OAuth2 login (Google), BCrypt, role-based authorization |
| Rate limiting | Bucket4j |
| Text extraction | Apache Tika |
| Observability | Micrometer → Prometheus → Grafana, Kibana over Elasticsearch |
| API docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, spring-security-test, JaCoCo |
| Build / Deploy | Maven, Jib (containerize without a Dockerfile), Docker Compose |

## API surface

| Controller | Base path | Responsibility |
|---|---|---|
| `AuthController` | `/api/auth` | Register, login, refresh, logout, role selection |
| `AdminController` | `/api/admin` | User/role management, invite flow, job moderation, analytics dashboards (skills, Kafka lag, applications) |
| `CompanyController` | `/api/companies` | Company creation/lookup |
| `JobController` | `/api/jobs` | Post/list/delete jobs, Elasticsearch-backed job search |
| `ResumeController` | `/api/resume` | Pre-signed upload URLs, upload registration, download URLs, "my resumes" |
| `JobApplicationController` | `/api/applications` | Apply to a job (rate-limited), view my applications, view applicants, update status, ranked-candidates view (recruiter-only) |
| `UserController` | `/api/user` | Save a job, profile, set active company |

Full request/response contracts are available at `/swagger-ui/index.html` once the app is running.

Role-based access (`CANDIDATE`, `RECRUITER`, `ADMIN`) is enforced both at the Spring Security filter-chain level (`hasRole(...)` matchers) and inside individual endpoints where the rule is data-dependent (e.g. only the job's recruiter can view its ranked candidates).

## Getting started

### Prerequisites
- Java 17+, Maven (or use the bundled `./mvnw`)
- Docker + Docker Compose

### Run the full stack locally

```bash
git clone <repo-url>
cd Hireflux
cp .env.example .env   # fill in DB, Redis, AWS, OpenAI, Google OAuth, mail credentials
docker compose up -d
```

This brings up: the app (`:8080`), Postgres (`:5432`), Adminer (`:8081`), Redis (`:6379`), Kafka (`:9092`), Elasticsearch (`:9200`), Kibana (`:5601`), Prometheus (`:9090`), and Grafana (`:3000`).

### Run just the app against local infra

```bash
docker compose up -d postgres redis kafka elasticsearch
./mvnw spring-boot:run
```

### Build

```bash
./mvnw clean package
```

## Configuration

All runtime configuration is environment-variable driven (see `docker-compose.yml` for the full list), grouped as: database, JPA, Redis, Kafka, Elasticsearch, AWS S3, mail (SMTP), Google OAuth2 client credentials, OpenAI API key, and admin-seeder credentials (bootstraps a default `ADMIN` user on first boot).

## Testing

```bash
./mvnw test
```

Unit tests cover services, controllers, matching strategies, and utility classes with JUnit 5 + Mockito, including Spring Data projection interfaces, WebClient fluent-chain mocking for the OpenAI client, and Spring Security test support for authenticated-endpoint tests. Coverage is tracked via JaCoCo (`./mvnw verify` generates the report under `target/site/jacoco`).

## Observability

- **Metrics**: Micrometer counters/timers (resume upload/success/failure/retry/DLQ counts, etc.) exposed at `/actuator/prometheus`, scraped by Prometheus, visualized in Grafana.
- **Logs**: structured `event=<name>, key=value` log lines at every meaningful state transition (resume lifecycle, Kafka publish success/failure), designed to be greppable/queryable rather than free-text prose.
- **Health**: `/actuator/health` is public (used by Compose healthchecks); full actuator detail is admin-visible.
- **Search visibility**: Kibana sits over Elasticsearch for ad-hoc inspection of indexed job documents.

## Known limitations / roadmap

- CORS currently allows all origins (`addAllowedOriginPattern("*")`) with credentials enabled — fine for local development, needs to be locked down to explicit origins before any public deployment.
- Strategy weights in the matching engine are hardcoded and not validated to sum to 1.0 at startup.
- `SkillMatchingStrategy` scores on the single best pairwise skill match rather than aggregate coverage of all required skills — a candidate strong in one overlapping skill currently scores the same as one covering every required skill.
- No API gateway / BFF layer yet — the service is consumed directly; a gateway would be the natural next step for multi-service growth.

## License

This project is licensed under the [Apache License 2.0](LICENSE).