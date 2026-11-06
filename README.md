# Transaction Processing Platform

> **Learning / portfolio project** — Java Spring Boot backend for understanding payment API flows and payment-system reliability. Not certified for, or suitable for, processing real money.

## Purpose
Build an interview-ready backend system step by step, while explaining *why* real payment systems need idempotency, transactional correctness, reconciliation and operational observability. Use this project for hands-on learning, not to claim production experience.

## Stack (confirm against source)
- Java 17
- Spring Boot 3.x (previously set up with 3.5.7; check `pom.xml`)
- Maven
- PostgreSQL 14
- JUnit 5 / Spring Boot Test (verify test dependencies)

## Current baseline — verify in the repository
The learning baseline discussed so far includes:
- `POST /payments` — payment creation exercise
- `GET /health` — health endpoint exercise
- `payments` table concept: `id`, unique `idempotency_key`, `amount`, `currency`, `status`
- `PENDING` payment state

**These are context notes, not a verified inventory of the repo.** Ask Codex to inspect controllers, models, repositories, database migrations and tests; replace this section with what actually exists.

## Intended architecture (illustrative, not necessarily implemented)
```text
Client / API consumer
        |
        v
REST Controller (DTO validation)
        |
        v
Payment Service (business rules, idempotency, transaction boundary)
        |
        v
Repository / PostgreSQL (unique constraints, persistence)
```

Future modules: simulated provider adapter, lifecycle events, transactional outbox, webhook consumer, reconciliation, monitoring and safe deployment. These are **planned**, not implemented.

## Local setup (adapt commands to your repo)
1. Install Java 17, Maven (or use the Maven wrapper), and PostgreSQL 14.
2. Inspect `pom.xml` for dependencies and Java version.
3. Review `src/main/resources/application.properties` or `.yml` for database configuration. **Never commit real credentials.** Prefer environment variables or local ignored configuration.
4. Create a **local-only test database** according to the actual application configuration and migrations. Do not guess database names or credentials.
5. Run the app:
   ```bash
   ./mvnw spring-boot:run  # only if mvnw exists
   # otherwise: mvn spring-boot:run
   ```
6. Run tests:
   ```bash
   ./mvnw test  # only if mvnw exists
   # otherwise: mvn test
   ```
7. Inspect actual controller mappings before making requests; example **only if endpoints exist**:
   ```bash
   curl -i http://localhost:8080/health
   ```

## API documentation
Codex should inspect the real controllers and document each implemented endpoint using this format:

| Method | Path | Request | Response | Errors | Verified? |
|---|---|---|---|---|---|
| POST | `/payments` | Inspect DTO | Inspect implementation | Inspect validation | Not yet verified |
| GET | `/health` | None expected | Inspect implementation | Inspect | Not yet verified |

Provide sanitized `curl` examples only after verifying endpoint paths, JSON schema and configuration.

## Engineering goals
- Atomic and safe idempotent payment creation under concurrency
- Correct lifecycle distinctions: authorize, capture, void, refund
- Clear REST error semantics and testable validation
- Reliable event delivery (transactional outbox, retries, DLQ)
- Reconciliation, observability, deployment and failure recovery

## How to learn with Codex
- Read [`AGENTS.md`](AGENTS.md): coaching and change-control rules.
- Follow [`LEARNING_ROADMAP.md`](LEARNING_ROADMAP.md): staged exercises.
- In IntelliJ Codex chat, type: `NEXT. Inspect this repository first. Teach only the next small task. Do not change files.`
- After coding yourself, type: `REVIEW my changes. Point out edge cases and tests. Don't edit.`
- If stuck, type `HINT 1`; use `HINT 2/3` only when needed.

## Project integrity
- No live payment processing, PAN/card data, customer data or real API keys.
- No `.env` / database passwords in Git.
- No fabricated performance numbers, reliability claims or test results in this README or CV.

## Progress
Track completed tasks and evidence in `LEARNING_ROADMAP.md`. Treat anything not verified by source/tests as planned or unknown.
