package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService();

    @Test
    void createsPendingPaymentWithRequestedAmountAndCurrency() {
        PaymentRequest request = new PaymentRequest(1000, "AUD");

        PaymentResponse response = service.createPayment(request, "new-payment");

        assertAll(
                () -> assertTrue(response.id().startsWith("pay_")),
                () -> assertEquals(1000L, response.amount()),
                () -> assertEquals("AUD", response.currency()),
                () -> assertEquals("PENDING", response.status())
        );
    }

    @Test
    void sameKeyAndEquivalentPayloadReturnOriginalPayment() {
        PaymentResponse original = service.createPayment(
                new PaymentRequest(1000, "AUD"), "retry-payment");

        PaymentResponse retry = service.createPayment(
                new PaymentRequest(1000, "AUD"), "retry-payment");

        assertEquals(original.id(), retry.id());
        assertEquals(original, retry);
    }

    @Test
    void sameKeyAndDifferentAmountRejectConflictAndPreserveOriginalPayment() {
        PaymentRequest originalRequest = new PaymentRequest(1000, "AUD");
        PaymentResponse original = service.createPayment(originalRequest, "amount-conflict");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPayment(new PaymentRequest(2000, "AUD"), "amount-conflict"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(original, service.createPayment(originalRequest, "amount-conflict"));
    }

    @Test
    void sameKeyAndDifferentCurrencyRejectConflictAndPreserveOriginalPayment() {
        PaymentRequest originalRequest = new PaymentRequest(1000, "AUD");
        PaymentResponse original = service.createPayment(originalRequest, "currency-conflict");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPayment(new PaymentRequest(1000, "USD"), "currency-conflict"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(original, service.createPayment(originalRequest, "currency-conflict"));
    }

    @Test
    void differentKeysAndSamePayloadCreateSeparatePayments() {
        PaymentRequest request = new PaymentRequest(1000, "AUD");

        PaymentResponse first = service.createPayment(request, "first-payment");
        PaymentResponse second = service.createPayment(request, "second-payment");

        assertNotEquals(first.id(), second.id());
    }

    @Test
    void currencyComparisonIsCaseSensitive() {
        PaymentRequest original = new PaymentRequest(1000, "AUD");
        PaymentResponse response = service.createPayment(original, "case-sensitive");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPayment(new PaymentRequest(1000, "aud"), "case-sensitive"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(response, service.createPayment(original, "case-sensitive"));
    }

    @Test
    void concurrentMatchingRequestsReturnOnePayment() throws Exception {
        List<Callable<PaymentResponse>> requests = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            requests.add(() -> service.createPayment(new PaymentRequest(1000, "AUD"), "shared-key"));
        }

        List<PaymentResponse> responses = runConcurrently(requests);

        PaymentResponse original = responses.get(0);
        for (PaymentResponse response : responses) {
            assertEquals(original, response);
        }
        assertEquals(original, service.createPayment(new PaymentRequest(1000, "AUD"), "shared-key"));
    }

    @Test
    void concurrentConflictingRequestsAcceptOnePayloadAndPreserveWinner() throws Exception {
        PaymentRequest first = new PaymentRequest(1000, "AUD");
        PaymentRequest second = new PaymentRequest(2000, "AUD");
        List<Callable<PaymentResponse>> requests = new ArrayList<>();
        for (PaymentRequest request : List.of(first, second)) {
            requests.add(() -> {
                try {
                    return service.createPayment(request, "contested-key");
                } catch (ResponseStatusException exception) {
                    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
                    return null;
                }
            });
        }

        List<PaymentResponse> responses = runConcurrently(requests);

        assertEquals(1L, responses.stream().filter(response -> response != null).count());
        PaymentResponse winner = responses.stream().filter(response -> response != null)
                .findFirst().orElseThrow();
        PaymentRequest winningRequest = winner.amount() == first.amount() ? first : second;
        PaymentRequest losingRequest = winner.amount() == first.amount() ? second : first;
        assertEquals(winner, service.createPayment(winningRequest, "contested-key"));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.createPayment(losingRequest, "contested-key"));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    private <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for concurrent start");
                    }
                    return task.call();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Workers must be ready before release");
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Workers must terminate");
        }
    }
}
