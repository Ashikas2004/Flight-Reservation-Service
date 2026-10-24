package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.BookingRequest;
import com.flightreservation.dto.request.CancelBookingRequest;
import com.flightreservation.dto.request.PassengerRequest;
import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.dto.response.PageResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private BookingResponse createMockBookingResponse(String ref, String status) {
        return BookingResponse.builder()
                .id(1L)
                .bookingReference(ref)
                .status(status)
                .totalAmount(new BigDecimal("500.00"))
                .bookedAt(LocalDateTime.now())
                .flight(BookingResponse.FlightSummary.builder()
                        .id(10L)
                        .flightNumber("AI-101")
                        .airlineName("Air India")
                        .originIata("DEL")
                        .destinationIata("BOM")
                        .build())
                .passengers(List.of(BookingResponse.PassengerInfo.builder()
                        .id(1L)
                        .firstName("Jane")
                        .lastName("Doe")
                        .seatNumber("12A")
                        .seatClass("ECONOMY")
                        .passportNumber("P1234567")
                        .build()))
                .build();
    }

    @Nested
    @DisplayName("POST /bookings")
    class CreateBookingTests {

        @Test
        @DisplayName("Success - returns 201 Created with booking details")
        void create_Success() throws Exception {
            BookingRequest req = new BookingRequest();
            req.setFlightId(10L);

            PassengerRequest passenger = new PassengerRequest();
            passenger.setSeatId(101L);
            passenger.setFirstName("Jane");
            passenger.setLastName("Doe");
            passenger.setPassportNumber("P1234567");
            passenger.setDateOfBirth("1990-01-01");
            passenger.setNationality("Indian");
            req.setPassengers(List.of(passenger));

            when(bookingService.createBooking(any(BookingRequest.class)))
                    .thenReturn(createMockBookingResponse("BK-123456", "PENDING"));

            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.bookingReference", is("BK-123456")))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.totalAmount", is(500.00)));

            verify(bookingService, times(1)).createBooking(any(BookingRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing flightId returns 400 Bad Request")
        void create_MissingFlightId_Returns400() throws Exception {
            BookingRequest req = new BookingRequest();
            req.setPassengers(List.of(new PassengerRequest()));

            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.flightId", notNullValue()));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("Validation Error - empty passenger list returns 400 Bad Request")
        void create_EmptyPassengers_Returns400() throws Exception {
            BookingRequest req = new BookingRequest();
            req.setFlightId(10L);
            req.setPassengers(Collections.emptyList());

            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.passengers", notNullValue()));

            verify(bookingService, never()).createBooking(any());
        }

        @Test
        @DisplayName("Conflict - seat already booked returns 409 Conflict")
        void create_SeatConflict_Returns409() throws Exception {
            BookingRequest req = new BookingRequest();
            req.setFlightId(10L);
            PassengerRequest passenger = new PassengerRequest();
            passenger.setSeatId(101L);
            passenger.setFirstName("Jane");
            passenger.setLastName("Doe");
            passenger.setPassportNumber("P1234567");
            passenger.setDateOfBirth("1990-01-01");
            passenger.setNationality("Indian");
            req.setPassengers(List.of(passenger));

            when(bookingService.createBooking(any(BookingRequest.class)))
                    .thenThrow(ApiException.conflict("Seat is already reserved"));

            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Seat is already reserved")));
        }

        @Test
        @DisplayName("Not Found - flight does not exist returns 404 Not Found")
        void create_FlightNotFound_Returns404() throws Exception {
            BookingRequest req = new BookingRequest();
            req.setFlightId(999L);
            PassengerRequest passenger = new PassengerRequest();
            passenger.setSeatId(101L);
            passenger.setFirstName("Jane");
            passenger.setLastName("Doe");
            passenger.setPassportNumber("P1234567");
            passenger.setDateOfBirth("1990-01-01");
            passenger.setNationality("Indian");
            req.setPassengers(List.of(passenger));

            when(bookingService.createBooking(any(BookingRequest.class)))
                    .thenThrow(ApiException.notFound("Flight not found: 999"));

            mockMvc.perform(post("/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Flight not found: 999")));
        }
    }

    @Nested
    @DisplayName("GET /bookings/my")
    class GetMyBookingsTests {

        @Test
        @DisplayName("Success - returns paginated user bookings")
        void getMyBookings_Success() throws Exception {
            BookingResponse booking = createMockBookingResponse("BK-1001", "CONFIRMED");
            PageResponse<BookingResponse> page = PageResponse.<BookingResponse>builder()
                    .content(List.of(booking))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(bookingService.getMyBookings(0, 10)).thenReturn(page);

            mockMvc.perform(get("/bookings/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].bookingReference", is("BK-1001")));

            verify(bookingService, times(1)).getMyBookings(0, 10);
        }

        @Test
        @DisplayName("Success - defaults page to 0 and size to 10 if not provided")
        void getMyBookings_DefaultPagination() throws Exception {
            PageResponse<BookingResponse> page = PageResponse.<BookingResponse>builder()
                    .content(Collections.emptyList())
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            when(bookingService.getMyBookings(0, 10)).thenReturn(page);

            mockMvc.perform(get("/bookings/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));

            verify(bookingService, times(1)).getMyBookings(0, 10);
        }
    }

    @Nested
    @DisplayName("GET /bookings/{reference}")
    class GetBookingByReferenceTests {

        @Test
        @DisplayName("Success - returns booking for valid reference")
        void getByReference_Success() throws Exception {
            BookingResponse booking = createMockBookingResponse("BK-9999", "CONFIRMED");
            when(bookingService.getByReference("BK-9999")).thenReturn(booking);

            mockMvc.perform(get("/bookings/{reference}", "BK-9999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingReference", is("BK-9999")))
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));

            verify(bookingService, times(1)).getByReference("BK-9999");
        }

        @Test
        @DisplayName("Failure - reference not found returns 404")
        void getByReference_NotFound_Returns404() throws Exception {
            when(bookingService.getByReference("UNKNOWN-REF"))
                    .thenThrow(ApiException.notFound("Booking not found: UNKNOWN-REF"));

            mockMvc.perform(get("/bookings/{reference}", "UNKNOWN-REF"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Booking not found: UNKNOWN-REF")));
        }
    }

    @Nested
    @DisplayName("PUT /bookings/{reference}/cancel")
    class CancelBookingTests {

        @Test
        @DisplayName("Success - cancels booking and returns 200 OK")
        void cancelBooking_Success() throws Exception {
            CancelBookingRequest cancelReq = new CancelBookingRequest();
            cancelReq.setReason("Change of plans");

            BookingResponse cancelledBooking = createMockBookingResponse("BK-8888", "CANCELLED");
            cancelledBooking.setCancellationReason("Change of plans");

            when(bookingService.cancelBooking(eq("BK-8888"), any(CancelBookingRequest.class)))
                    .thenReturn(cancelledBooking);

            mockMvc.perform(put("/bookings/{reference}/cancel", "BK-8888")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingReference", is("BK-8888")))
                    .andExpect(jsonPath("$.status", is("CANCELLED")));

            verify(bookingService, times(1)).cancelBooking(eq("BK-8888"), any(CancelBookingRequest.class));
        }

        @Test
        @DisplayName("Business Rule Violation - already cancelled booking returns 400 Bad Request")
        void cancelBooking_AlreadyCancelled_Returns400() throws Exception {
            CancelBookingRequest cancelReq = new CancelBookingRequest();
            cancelReq.setReason("Duplicate cancellation");

            when(bookingService.cancelBooking(eq("BK-8888"), any(CancelBookingRequest.class)))
                    .thenThrow(ApiException.badRequest("Booking is already cancelled"));

            mockMvc.perform(put("/bookings/{reference}/cancel", "BK-8888")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Booking is already cancelled")));
        }

        @Test
        @DisplayName("Not Found - booking reference not found returns 404")
        void cancelBooking_NotFound_Returns404() throws Exception {
            CancelBookingRequest cancelReq = new CancelBookingRequest();

            when(bookingService.cancelBooking(eq("NONEXISTENT"), any(CancelBookingRequest.class)))
                    .thenThrow(ApiException.notFound("Booking not found: NONEXISTENT"));

            mockMvc.perform(put("/bookings/{reference}/cancel", "NONEXISTENT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelReq)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Booking not found: NONEXISTENT")));
        }
    }
}

