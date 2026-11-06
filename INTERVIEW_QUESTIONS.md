# Payments Backend Interview Questions

## Session 1: Payment Idempotency and Safe Retries

### Main question

A client calls `POST /payments` and the request times out. How should the client retry to avoid creating a duplicate payment?

### Follow-up questions

1. Does a client timeout prove that the server failed to create the payment? Why or why not?
2. Which idempotency key and payload should the client use when retrying?
3. What happens in the current implementation if the client retries with a new idempotency key and the same payload?
4. What happens when the client reuses an idempotency key with a different amount or currency?
5. How does a payment ID differ from an idempotency key?
6. Does the current `ConcurrentHashMap` implementation preserve idempotency after an application restart? Explain why.
7. If two application instances receive requests with the same idempotency key, can the current implementation prevent duplicate payments across both instances?
8. How would you make idempotency durable and safe across multiple application instances?

### Practice task

Answer the main question in two or three sentences, then explain the limitations of the current implementation using `PaymentController`, `PaymentService`, and `PaymentRecord`.

### Key concepts: persistence and atomic uniqueness

- **Persistence** preserves the idempotency key and its associated payment record across application restarts, allowing a retry to find the original payment.
- **Atomic uniqueness** prevents concurrent requests from creating duplicate records for the same idempotency key. A database unique constraint enforces this across application instances; a separate check followed by an insert is vulnerable to a race condition.
- These solve different problems: durable storage alone does not prevent concurrent duplicates, and an atomic in-memory map operation does not preserve records after a restart.

### Additional interview question

Two application instances both query the database for the same idempotency key and find no record. Why can both then attempt to create a payment? How would a unique constraint and an atomic transaction boundary help, and how should the application handle the losing request?

The questions remain open for future practice; the notes above capture concepts discussed in this session.

## Session 2: Service Instances, Concurrency, and Test Boundaries

### Interview questions

1. How does a service instance differ from a physical server, an application process, and a pinpad client?
2. Two pinpads send requests simultaneously to one backend application. Can those requests share one service instance and still execute concurrently?
3. Why does `ConcurrentHashMap.computeIfAbsent()` protect same-key creation within one service instance but not across separate application processes?
4. Why must a client reuse the original idempotency key and payload after a timeout?
5. What do `@Positive`, `@NotBlank`, and controller-level `@Valid` enforce? Should an invalid amount reach the payment service?
6. What does `verifyNoInteractions(service)` prove in a controller validation test?
7. Can a controller test with a mocked payment service detect broken idempotency logic in the real service? Why or why not?
8. If every retry incorrectly generates a new payment ID, which existing service test should fail, and which assertion detects it?
9. Does a successful sequential retry test prove safety under concurrent requests or across application instances? What additional evidence would you need?

### Reference notes

- A service instance is a Java object. With the current Spring configuration, requests within one application context share the singleton `PaymentService` bean and its map. Separate application processes have separate maps, even on the same physical server.
- Concurrency means request execution overlaps. It can occur within one instance or across multiple instances; client count does not determine service instance count.
- `computeIfAbsent()` executes the creation function only when the key is absent. Within the same map, a matching retry reads the existing record instead of generating a new ID. Different keys represent separate operations, even when their payloads match.
- `@Positive` requires an amount greater than zero. `@NotBlank` rejects null, empty, and whitespace-only currency values. Controller `@Valid` triggers Bean Validation for the request DTO.
- Controller tests use MockMvc with a mocked service to verify HTTP binding, validation, delegation, and response serialization. `verifyNoInteractions(service)` confirms rejected requests never invoked the mocked service.
- A mocked service does not execute real idempotency logic. Controller tests can still pass if that logic is broken while the service method signature remains unchanged.
- `sameKeyAndEquivalentPayloadReturnOriginalPayment()` calls the same real service instance twice with equivalent payloads and the same key. `assertEquals(original.id(), retry.id())` fails if the retry generates a new ID.
- Current evidence: eight service tests and twenty controller test cases pass (28 total). Service tests include coordinated concurrent requests within one instance. These tests do not establish cross-instance uniqueness or restart persistence, and a passing concurrency test does not exhaust all possible thread schedules.

### Practice task

Draw two clients calling one application process, then two clients calling separate application processes. Mark which requests share a map. Explain what each existing test suite proves and what remains untested.

## Session 3: Concurrent Test Outcomes and Validation Boundaries

### Interview questions and reference answers

1. **Why should a concurrent conflicting-payload test avoid requiring a specific payload to win?**
   Thread scheduling does not guarantee which request reaches the atomic operation first. Assert that exactly one request succeeds, the other receives a conflict, and the winning record remains unchanged, regardless of which payload wins.
2. **Why verify that an invalid request never invokes the service, in addition to asserting HTTP 400?**
   Invalid input should be rejected at the API boundary before business processing. A 400 response alone does not prove the service was never invoked or that payment creation was never attempted. `verifyNoInteractions(service)` checks this boundary in the controller test.
3. **Why is a database unique constraint still needed after all 28 tests pass?**
   The concurrent service tests share one service object and one map. Separate application processes have separate maps and can both accept the same key. A shared database unique constraint atomically rejects duplicate key insertion across those processes; the application must handle the conflict and retrieve the original result when appropriate.
4. **Should a blank idempotency key return HTTP 400 or 409?**
   Return 400 because the request is invalid. Use 409 when an existing key is reused with a conflicting payload. Requiring the header to be present does not by itself reject an empty or whitespace-only value.
