# Learning Roadmap — Payments Backend / Platform SWE

## How to use
Each session is 20–45 minutes: **concept (5–10 min) → implement yourself (15–25 min) → test/review (10 min)**. Start by inspecting the real code. Do not skip a stage merely because a topic sounds familiar. A topic is complete when you can explain the trade-offs and tests yourself.

**Progress markers:** `[ ]` not started, `[~]` in progress, `[x]` verified with implementation + tests + learner explanation. Update only with learner agreement.

## Stage 0 — Repository audit and working baseline
- [ ] Identify actual controllers/services/repositories/models/config/tests, and draw the request flow.
- [ ] Run a clean build and tests; note failures separately from setup problems.
- [ ] Document the real API and schema without inventing endpoints.
- [ ] Explain HTTP request → validation → service → DB → response.

**Proof:** explain what happens to a `POST /payments` request using real filenames.

## Stage 1 — REST and persistence fundamentals
- [ ] Exercise 1: validate amount, currency, and malformed JSON; define response codes.
- [ ] Exercise 2: separate request/response DTOs from persistence entities.
- [ ] Exercise 3: transaction scope and database unique constraints.
- [ ] Exercise 4: write unit tests and controller/integration tests.

**Interview check:** What is the difference between HTTP 400, 409, and 500 for a payment request?

## Stage 2 — Payment idempotency and concurrency
- [ ] Exercise 5: two same-key requests with same payload must produce one logical payment.
- [ ] Exercise 6: same key with conflicting payload — define and test conflict behavior.
- [ ] Exercise 7: simultaneous requests from two app instances; enforce uniqueness in the shared DB.
- [ ] Exercise 8: crash after DB commit, before HTTP response; safe retry and status retrieval.

**Proof:** demonstrate a concurrency test and explain why in-memory locks alone cannot guarantee correctness across instances.

## Stage 3 — Payment lifecycle and provider integration
- [ ] Exercise 9: distinguish authorization, capture, void and refund with a state diagram.
- [ ] Exercise 10: implement a mock/sandbox provider adapter (never send real card data).
- [ ] Exercise 11: map provider errors, timeouts, retries and uncertainty states.
- [ ] Exercise 12: define money representation, currency precision and partial refund rules.

**Interview check:** Why does authorization success not mean settlement has occurred?

## Stage 4 — Asynchronous reliability
- [ ] Exercise 13: demonstrate dual-write failure (DB commit succeeds, event publish fails).
- [ ] Exercise 14: transactional outbox with delivery worker.
- [ ] Exercise 15: deduplicate webhook/event delivery; handle out-of-order events.
- [ ] Exercise 16: bounded retries, backoff, poison message and DLQ.

**Proof:** recover after process crash without losing or duplicating business effects.

## Stage 5 — Reconciliation and settlement
- [ ] Exercise 17: build simulated acquirer/processor settlement records.
- [ ] Exercise 18: reconcile internal transactions against external records using stable identifiers.
- [ ] Exercise 19: handle missing records, duplicates, mismatches and replayed batches.
- [ ] Exercise 20: create an exception report with traceable actions.

**Interview check:** What if a batch was processed but its acknowledgement was lost?

## Stage 6 — Platform and production engineering
- [ ] Exercise 21: structured logs with correlation IDs, safe redaction, and health checks.
- [ ] Exercise 22: containerised local deployment and CI build/tests.
- [ ] Exercise 23: backward-compatible schema change and rollback thinking.
- [ ] Exercise 24: reason about active-active vs active-standby, replica lag, failover and disaster recovery.

**Proof:** describe the behavior and recovery plan for a failure during deployment.

## Stage 7 — Portfolio and interview readiness
- [ ] Add a verified architecture diagram, API examples and test guide to README.
- [ ] Demonstrate one meaningful integration test and one failure-recovery scenario.
- [ ] Write a concise English design decision record: choice, alternatives and trade-offs.
- [ ] Practice explaining the project in 60 seconds and defending one design in five minutes.

## Session template (Codex should follow)
**Topic:**
**Prerequisites verified in code:**
**Why payments engineers care:**
**One concept, briefly:**
**Task for learner (no full solution initially):**
**Acceptance criteria:**
**Hints available:**
**Tests / evidence:**
**2 recall questions + 1 interview question:**
**Next session:**

## First Codex prompt
> NEXT. First inspect pom.xml, relevant src/main and src/test, configuration, and git status. Give me a factual project baseline, identify which Stage 0 items are actually complete, then teach me one 30-minute task. Speak Hong Kong Cantonese with English technical terms. No file edits until I ask.
