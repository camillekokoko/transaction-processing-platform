package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
