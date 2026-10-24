package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.RefundRequest;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.dto.response.RefundResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.RefundService;
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
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundController refundController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(refundController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private RefundResponse createMockRefundResponse(Long bookingId, String status) {
        return RefundResponse.builder()
                .id(1L)
                .bookingId(bookingId)
                .bookingReference("BK-200")
                .refundAmount(new BigDecimal("450.00"))
                .currency("INR")
                .status(status)
                .reason("Flight cancelled by passenger")
                .refundTransactionId("REF-987654")
                .createdAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("POST /refunds")
    class RequestRefundTests {

        @Test
        @DisplayName("Success - processes refund request and returns 200 OK")
        void requestRefund_Success() throws Exception {
            RefundRequest req = new RefundRequest();
            req.setBookingId(1L);
            req.setReason("Flight cancelled");

            RefundResponse resp = createMockRefundResponse(1L, "PROCESSED");
            when(refundService.requestRefund(any(RefundRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/refunds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId", is(1)))
                    .andExpect(jsonPath("$.status", is("PROCESSED")))
                    .andExpect(jsonPath("$.refundTransactionId", is("REF-987654")));

            verify(refundService, times(1)).requestRefund(any(RefundRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing bookingId returns 400 Bad Request")
        void requestRefund_MissingBookingId_Returns400() throws Exception {
            RefundRequest req = new RefundRequest();

            mockMvc.perform(post("/refunds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.bookingId", notNullValue()));

            verify(refundService, never()).requestRefund(any());
        }

        @Test
        @DisplayName("Business Rule Violation - booking not cancelled returns 400 Bad Request")
        void requestRefund_NotCancelled_Returns400() throws Exception {
            RefundRequest req = new RefundRequest();
            req.setBookingId(1L);

            when(refundService.requestRefund(any(RefundRequest.class)))
                    .thenThrow(ApiException.badRequest("Refund can only be requested for cancelled bookings"));

            mockMvc.perform(post("/refunds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Refund can only be requested for cancelled bookings")));
        }

        @Test
        @DisplayName("Not Found - booking does not exist returns 404")
        void requestRefund_BookingNotFound_Returns404() throws Exception {
            RefundRequest req = new RefundRequest();
            req.setBookingId(999L);

            when(refundService.requestRefund(any(RefundRequest.class)))
                    .thenThrow(ApiException.notFound("Booking not found: 999"));

            mockMvc.perform(post("/refunds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Booking not found: 999")));
        }
    }

    @Nested
    @DisplayName("GET /refunds/my")
    class GetMyRefundsTests {

        @Test
        @DisplayName("Success - returns paginated refund records for current user")
        void getMyRefunds_Success() throws Exception {
            RefundResponse resp = createMockRefundResponse(1L, "PROCESSED");
            PageResponse<RefundResponse> page = PageResponse.<RefundResponse>builder()
                    .content(List.of(resp))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(refundService.getMyRefunds(0, 10)).thenReturn(page);

            mockMvc.perform(get("/refunds/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].bookingReference", is("BK-200")));

            verify(refundService, times(1)).getMyRefunds(0, 10);
        }

        @Test
        @DisplayName("Success - default pagination when no params supplied")
        void getMyRefunds_DefaultParams() throws Exception {
            PageResponse<RefundResponse> page = PageResponse.<RefundResponse>builder()
                    .content(Collections.emptyList())
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            when(refundService.getMyRefunds(0, 10)).thenReturn(page);

            mockMvc.perform(get("/refunds/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /refunds/booking/{bookingId}")
    class GetRefundByBookingTests {

        @Test
        @DisplayName("Success - returns refund info for booking")
        void getByBooking_Success() throws Exception {
            RefundResponse resp = createMockRefundResponse(1L, "PROCESSED");
            when(refundService.getByBookingId(1L)).thenReturn(resp);

            mockMvc.perform(get("/refunds/booking/{bookingId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingId", is(1)))
                    .andExpect(jsonPath("$.status", is("PROCESSED")));

            verify(refundService, times(1)).getByBookingId(1L);
        }

        @Test
        @DisplayName("Not Found - refund record not found returns 404")
        void getByBooking_NotFound_Returns404() throws Exception {
            when(refundService.getByBookingId(999L))
                    .thenThrow(ApiException.notFound("Refund not found for booking: 999"));

            mockMvc.perform(get("/refunds/booking/{bookingId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Refund not found for booking: 999")));
        }
    }
}

