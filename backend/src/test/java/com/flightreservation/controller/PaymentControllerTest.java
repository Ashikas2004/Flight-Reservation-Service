package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.PaymentRequest;
import com.flightreservation.dto.response.PaymentResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PaymentResponse createMockPaymentResponse(Long bookingId, String status) {
        return PaymentResponse.builder()
                .id(1L)
                .bookingId(bookingId)
                .bookingReference("BK-100")
                .transactionId("TXN-123456")
                .amount(new BigDecimal("499.99"))
                .currency("INR")
                .paymentMethod("CREDIT_CARD")
                .status(status)
                .paidAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /payments")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Success - processes payment and returns 200 OK")
        void processPayment_Success() throws Exception {
            PaymentRequest req = new PaymentRequest();
            req.setBookingId(1L);
            req.setPaymentMethod("CREDIT_CARD");
            req.setCardNumber("1234567890123456");
            req.setCardHolderName("John Doe");
            req.setExpiryMonth("12");
            req.setExpiryYear("2028");
            req.setCvv("123");

            PaymentResponse resp = createMockPaymentResponse(1L, "SUCCESS");
            when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId", is(1)))
                    .andExpect(jsonPath("$.status", is("SUCCESS")))
                    .andExpect(jsonPath("$.transactionId", is("TXN-123456")));

            verify(paymentService, times(1)).processPayment(any(PaymentRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing bookingId returns 400 Bad Request")
        void processPayment_MissingBookingId_Returns400() throws Exception {
            PaymentRequest req = new PaymentRequest();
            req.setPaymentMethod("CREDIT_CARD");

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.bookingId", notNullValue()));

            verify(paymentService, never()).processPayment(any());
        }

        @Test
        @DisplayName("Validation Error - missing paymentMethod returns 400 Bad Request")
        void processPayment_MissingPaymentMethod_Returns400() throws Exception {
            PaymentRequest req = new PaymentRequest();
            req.setBookingId(1L);

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.paymentMethod", notNullValue()));

            verify(paymentService, never()).processPayment(any());
        }

        @Test
        @DisplayName("Not Found - booking not found returns 404")
        void processPayment_BookingNotFound_Returns404() throws Exception {
            PaymentRequest req = new PaymentRequest();
            req.setBookingId(999L);
            req.setPaymentMethod("UPI");

            when(paymentService.processPayment(any(PaymentRequest.class)))
                    .thenThrow(ApiException.notFound("Booking not found: 999"));

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Booking not found: 999")));
        }

        @Test
        @DisplayName("Business Rule Violation - booking already paid returns 400")
        void processPayment_AlreadyPaid_Returns400() throws Exception {
            PaymentRequest req = new PaymentRequest();
            req.setBookingId(1L);
            req.setPaymentMethod("UPI");

            when(paymentService.processPayment(any(PaymentRequest.class)))
                    .thenThrow(ApiException.badRequest("Booking is already paid"));

            mockMvc.perform(post("/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Booking is already paid")));
        }
    }

    @Nested
    @DisplayName("GET /payments/booking/{bookingId}")
    class GetPaymentByBookingTests {

        @Test
        @DisplayName("Success - returns payment info for booking")
        void getByBooking_Success() throws Exception {
            PaymentResponse resp = createMockPaymentResponse(1L, "SUCCESS");
            when(paymentService.getPaymentByBookingId(1L)).thenReturn(resp);

            mockMvc.perform(get("/payments/booking/{bookingId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId", is(1)))
                    .andExpect(jsonPath("$.status", is("SUCCESS")));

            verify(paymentService, times(1)).getPaymentByBookingId(1L);
        }

        @Test
        @DisplayName("Not Found - no payment record found for booking returns 404")
        void getByBooking_NotFound_Returns404() throws Exception {
            when(paymentService.getPaymentByBookingId(999L))
                    .thenThrow(ApiException.notFound("Payment not found for booking: 999"));

            mockMvc.perform(get("/payments/booking/{bookingId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Payment not found for booking: 999")));
        }
    }
}

