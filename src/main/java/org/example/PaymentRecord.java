package org.example;

public record PaymentRecord(
        PaymentRequest request,
        PaymentResponse response
) {}