package com.flightreservation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Simulates an external payment gateway.
 * In production, replace this with a real payment provider SDK (Stripe, PayPal, etc.)
 */
@Component
@Slf4j
public class MockPaymentGateway {

    @Value("${app.payment.mock-success-rate:0.95}")
    private double successRate;

    public GatewayResult charge(String paymentMethod, BigDecimal amount, String currency,
                                 Map<String, String> metadata) {
        log.info("MockPaymentGateway: processing {} {} via {}", amount, currency, paymentMethod);

        // Simulate processing delay
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        boolean success = Math.random() < successRate;
        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        if (success) {
            log.info("MockPaymentGateway: SUCCESS transactionId={}", transactionId);
            return GatewayResult.success(transactionId, "{\"status\":\"approved\",\"code\":\"00\"}");
        } else {
            log.warn("MockPaymentGateway: FAILED - insufficient funds simulation");
            return GatewayResult.failure("INSUFFICIENT_FUNDS", "Payment declined by issuing bank");
        }
    }

    public GatewayResult refund(String originalTransactionId, BigDecimal amount, String currency) {
        log.info("MockPaymentGateway: processing refund for txn={} amount={}", originalTransactionId, amount);
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        String refundTransactionId = "RFD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("MockPaymentGateway: Refund SUCCESS refundTxnId={}", refundTransactionId);
        return GatewayResult.success(refundTransactionId, "{\"status\":\"refunded\"}");
    }

    public record GatewayResult(boolean success, String transactionId, String gatewayResponse,
                                 String errorCode, String errorMessage) {
        public static GatewayResult success(String transactionId, String response) {
            return new GatewayResult(true, transactionId, response, null, null);
        }

        public static GatewayResult failure(String errorCode, String errorMessage) {
            return new GatewayResult(false, null, null, errorCode, errorMessage);
        }
    }
}
