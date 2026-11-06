package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService service;

    @Test
    void validRequestReturnsCreatedPayment() throws Exception {
        PaymentRequest request = new PaymentRequest(1000, "AUD");
        when(service.createPayment(request, "test-001"))
                .thenReturn(new PaymentResponse("pay_test", 1000, "AUD", "PENDING"));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000,"currency":"AUD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay_test"))
                .andExpect(jsonPath("$.amount").value(1000))
                .andExpect(jsonPath("$.currency").value("AUD"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(service).createPayment(request, "test-001");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amount\":0,\"currency\":\"AUD\"}",
            "{\"amount\":-100,\"currency\":\"AUD\"}",
            "{\"amount\":1000,\"currency\":\"   \"}",
            "{\"amount\":1000,\"currency\":null}",
            "{\"amount\":1000,\"currency\":\"\"}",
            "{\"amount\":1000}",
            "{\"currency\":\"AUD\"}",
            "{\"amount\":\"invalid\",\"currency\":\"AUD\"}",
            "{\"amount\":9223372036854775808,\"currency\":\"AUD\"}",
            "{}",
            "null",
            "",
            "{\"amount\":1000,\"currency\":\"AUD\""
    })
    void invalidBodyReturnsBadRequestWithoutCallingService(String body) throws Exception {
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "test-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void missingIdempotencyHeaderReturnsBadRequestWithoutCallingService() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":1000,"currency":"AUD"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void healthReturnsFixedMessageWithoutCallingService() throws Exception {
        mockMvc.perform(get("/payments/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Payment API is running"));

        verifyNoInteractions(service);
    }

    @Test
    void serviceConflictReturnsConflictStatus() throws Exception {
        PaymentRequest request = new PaymentRequest(2000, "AUD");
        when(service.createPayment(request, "used-key"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key already used with a different request"));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "used-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2000,\"currency\":\"AUD\"}"))
                .andExpect(status().isConflict());

        verify(service).createPayment(request, "used-key");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void positiveAmountBoundariesReachService(long amount) throws Exception {
        PaymentRequest request = new PaymentRequest(amount, "AUD");
        when(service.createPayment(request, "boundary-key"))
                .thenReturn(new PaymentResponse("pay_boundary", amount, "AUD", "PENDING"));

        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "boundary-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":" + amount + ",\"currency\":\"AUD\"}"))
                .andExpect(status().isCreated());

        verify(service).createPayment(request, "boundary-key");
    }

    @Test
    void unsupportedContentTypeReturnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/payments")
                        .header("Idempotency-Key", "test-001")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("payment"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(service);
    }
}
