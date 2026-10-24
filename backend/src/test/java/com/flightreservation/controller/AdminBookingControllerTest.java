package com.flightreservation.controller;

import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("GET /admin/bookings/{reference}")
    class GetBookingTests {

        @Test
        @DisplayName("Success - admin retrieves booking by reference")
        void getBooking_Success() throws Exception {
            BookingResponse booking = BookingResponse.builder()
                    .id(1L)
                    .bookingReference("BK-ADMIN-1")
                    .status("CONFIRMED")
                    .totalAmount(new BigDecimal("1200.00"))
                    .bookedAt(LocalDateTime.now())
                    .build();

            when(bookingService.getByReference("BK-ADMIN-1")).thenReturn(booking);

            mockMvc.perform(get("/admin/bookings/{reference}", "BK-ADMIN-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingReference", is("BK-ADMIN-1")))
                    .andExpect(jsonPath("$.status", is("CONFIRMED")))
                    .andExpect(jsonPath("$.totalAmount", is(1200.00)));

            verify(bookingService, times(1)).getByReference("BK-ADMIN-1");
        }

        @Test
        @DisplayName("Not Found - booking reference not found returns 404")
        void getBooking_NotFound_Returns404() throws Exception {
            when(bookingService.getByReference("UNKNOWN-REF"))
                    .thenThrow(ApiException.notFound("Booking not found: UNKNOWN-REF"));

            mockMvc.perform(get("/admin/bookings/{reference}", "UNKNOWN-REF"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Booking not found: UNKNOWN-REF")));

            verify(bookingService, times(1)).getByReference("UNKNOWN-REF");
        }
    }
}

