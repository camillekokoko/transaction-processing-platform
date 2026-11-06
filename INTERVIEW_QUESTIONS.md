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

Answers are intentionally left open for future practice.
