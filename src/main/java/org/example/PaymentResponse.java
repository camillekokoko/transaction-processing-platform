//Serialization：`PaymentResponse` → JSON
// DTO: server => client

package org.example;

public record PaymentResponse(
        String id,
        long amount,
        String currency,
        String status
) {}
