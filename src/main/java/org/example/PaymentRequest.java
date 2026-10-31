//Deserialization：JSON → PaymentRequest
//Data Transfer Object client => server
package org.example;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @Positive long amount,
        @NotBlank String currency
) {
}