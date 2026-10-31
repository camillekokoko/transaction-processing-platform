package org.example;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final Map<String, PaymentRecord> payments =
            new ConcurrentHashMap<>();


    public PaymentResponse createPayment(
            PaymentRequest request,
            String idempotencyKey
    ) {
        PaymentRecord record = payments.computeIfAbsent(idempotencyKey, key -> {

            String paymentId = "pay_" + UUID.randomUUID();

            PaymentResponse response = new PaymentResponse(
                    paymentId,
                    request.amount(),
                    request.currency(),
                    "PENDING"
            );
            return new PaymentRecord(request, response);

        });

        if (!record.request().equals(request)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key already used with a different request"
            );
        }
        return record.response();
    }
}