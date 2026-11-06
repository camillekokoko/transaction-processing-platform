# Transaction Processing Platform

> **Learning / portfolio project** — Java Spring Boot backend for understanding payment API flows and payment-system reliability. Not certified for, or suitable for, processing real money.

## Purpose
Build an interview-ready backend system step by step, while explaining *why* real payment systems need idempotency, transactional correctness, reconciliation and operational observability. Use this project for hands-on learning, not to claim production experience.

## Stack

| Technology | Verified repository status |
|---|---|
| Java 17 | Compiler source and target configured in `pom.xml` |
| Spring Boot 3.5.7 | Parent version in `pom.xml` |
| Maven | Build configuration present; no Maven wrapper script |
| Spring Web / Jakarta Bean Validation | Used by the controller and request DTO |
| Spring Data JPA / PostgreSQL JDBC | Dependencies and connection configuration present; payment service does not use them |
| Spring Boot Test / JUnit Jupiter | Test dependency present; no test implementations |

Installed tool versions and PostgreSQL version have not been verified. PostgreSQL 14 is the intended learning environment.

## Current implementation

Verified by source inspection; build, application startup and HTTP behavior have not been verified in this audit.

- `POST /payments`: validate input and create a simulated payment with a `pay_<UUID>` ID and `PENDING` status.
- `GET /payments/health`: return the fixed text `Payment API is running`.
- In-memory idempotency using `ConcurrentHashMap.computeIfAbsent()`.
- Matching retries return the original response; conflicting key reuse returns HTTP `409`.

No persistence entities, repositories or database migrations exist in the repository. The payment flow does not write to PostgreSQL. The existence or structure of local database tables has not been verified.

## Current architecture

```text
HTTP client: JSON body + Idempotency-Key header
                         |
                         v
                 PaymentController
             deserialization + validation
                         |
                         v
                   PaymentService
             payment creation + idempotency
                         |
                         v
       ConcurrentHashMap<String, PaymentRecord>
           key -> original request + response
                         |
                         v
                PaymentResponse -> JSON
```

All application classes are in `src/main/java/org/example`:

| Class | Responsibility |
|---|---|
| `Main` | Start Spring Boot and component scanning |
| `PaymentController` | Map HTTP endpoints and delegate to the injected service |
| `PaymentRequest` | Input record containing `amount` and `currency`, with validation constraints |
| `PaymentService` | Generate IDs, store records and enforce idempotency rules |
| `PaymentRecord` | Pair the original request with its response; not a JPA entity |
| `PaymentResponse` | Output record containing `id`, `amount`, `currency` and `status` |

## API

Behavior below is derived from source and is not yet covered by automated tests.

| Method | Path | Request | Success | Errors |
|---|---|---|---|---|
| GET | `/payments/health` | None | `200`, fixed text | No custom error handling |
| POST | `/payments` | JSON body and required `Idempotency-Key` header | `201`, payment JSON, including matching retries | `400` for validation/binding errors or missing header; `409` for conflicting key reuse |

### Create a payment

Once the application is running locally:

```bash
curl -i http://localhost:8080/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: lesson-001' \
  -d '{"amount":1000,"currency":"AUD"}'
```

Example response body (ID varies):

```json
{
  "id": "pay_17472082-3f1c-47f3-87c9-b886369dabeb",
  "amount": 1000,
  "currency": "AUD",
  "status": "PENDING"
}
```

The learning convention is integer minor units: `1000` represents AUD 10.00. The code requires a positive `long` amount and a nonblank currency; it does not validate supported currency codes or currency-specific precision.

### Idempotency and retries

| Key | Request fields | Behavior within the same running application instance |
|---|---|---|
| New | Valid | Create a payment; return `201` |
| Existing | Same amount and currency | Return the original payment ID and response; return `201` |
| Existing | Different amount or currency | Return `409`; leave the original record unchanged |
| Different | Same amount and currency as an earlier request | Create a separate payment |

The client supplies the idempotency key in an HTTP header. The server generates the payment ID. They serve different purposes.

A client timeout does not prove payment creation failed. Retrying the same logical operation should reuse the original key and payload. Request comparison uses Java record equality, so currency comparison is case-sensitive.

### Health check

```bash
curl -i http://localhost:8080/payments/health
```

This endpoint does not check database connectivity or downstream services.

## Local setup and verification

1. Install Java 17 and Maven. There is no `mvnw` script in the repository.
2. Review `src/main/resources/application.properties`. It points to a local PostgreSQL database named `payment_db`. Adapt connection settings to your local learning environment; prefer environment variables or ignored local configuration for credentials.
3. Ensure the configured local PostgreSQL database is accessible. JPA auto-configuration may require a working connection at startup even though payment storage is in memory. `spring.jpa.hibernate.ddl-auto=validate` does not create tables, and no migrations are provided.
4. Start the application:

   ```bash
   mvn spring-boot:run
   ```

5. Build and invoke the test phase:

   ```bash
   mvn clean test
   ```

No test implementations currently exist under `src/test`. A successful build alone is not evidence that payment behavior has been tested. These commands have not been run in this audit.

## Current limitations and planned work

- Records and idempotency state disappear when the application stops or restarts.
- Separate application instances do not share their maps. Atomic map operations protect creation only within one instance.
- The map has no expiry or cleanup.
- The idempotency header is required but has no explicit nonblank constraint.
- Durable storage, database uniqueness constraints and payment transaction boundaries are not implemented.
- Payments remain `PENDING`; authorization, capture, void, refund, settlement and status retrieval are not implemented.
- Provider integration, transactional outbox, webhook processing, retries and reconciliation are planned.
- Automated tests, authentication, containerization, CI and operational observability remain future work.

## Engineering goals
- Atomic and safe idempotent payment creation under concurrency
- Correct lifecycle distinctions: authorize, capture, void, refund
- Clear REST error semantics and testable validation
- Reliable event delivery (transactional outbox, retries, DLQ)
- Reconciliation, observability, deployment and failure recovery

## How to learn with Codex
- Read [`AGENTS.md`](AGENTS.md): coaching and change-control rules.
- Follow [`LEARNING_ROADMAP.md`](LEARNING_ROADMAP.md): staged exercises.
- Revisit [`INTERVIEW_QUESTIONS.md`](INTERVIEW_QUESTIONS.md): idempotency and safe retry practice.
- In IntelliJ Codex chat, type: `NEXT. Inspect this repository first. Teach only the next small task. Do not change files.`
- After coding yourself, type: `REVIEW my changes. Point out edge cases and tests. Don't edit.`
- If stuck, type `HINT 1`; use `HINT 2/3` only when needed.

## Project integrity
- No live payment processing, PAN/card data, customer data or real API keys.
- No `.env` / database passwords in Git.
- No fabricated performance numbers, reliability claims or test results in this README or CV.

## Progress
Track completed tasks and evidence in `LEARNING_ROADMAP.md`. Treat anything not verified by source/tests as planned or unknown.
