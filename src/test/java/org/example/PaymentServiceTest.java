package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
}
