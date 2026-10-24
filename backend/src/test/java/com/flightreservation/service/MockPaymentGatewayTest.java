package com.flightreservation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    @DisplayName("charge - succeeds when successRate is 1.0")
    void charge_Success() {
        ReflectionTestUtils.setField(gateway, "successRate", 1.0);

        MockPaymentGateway.GatewayResult result = gateway.charge("CREDIT_CARD", new BigDecimal("100.00"), "USD", Map.of("ref", "BK123"));

        assertNotNull(result);
        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("TXN-"));
    }

    @Test
    @DisplayName("charge - fails when successRate is 0.0")
    void charge_Failure() {
        ReflectionTestUtils.setField(gateway, "successRate", 0.0);

        MockPaymentGateway.GatewayResult result = gateway.charge("CREDIT_CARD", new BigDecimal("100.00"), "USD", Map.of());

        assertNotNull(result);
        assertFalse(result.success());
        assertEquals("INSUFFICIENT_FUNDS", result.errorCode());
    }

    @Test
    @DisplayName("refund - always succeeds with RFD- transactionId")
    void refund_Success() {
        MockPaymentGateway.GatewayResult result = gateway.refund("TXN-123456", new BigDecimal("100.00"), "USD");

        assertNotNull(result);
        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("RFD-"));
    }
}

