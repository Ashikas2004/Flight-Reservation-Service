package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.SeatLockRequest;
import com.flightreservation.dto.request.SeatReleaseRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.SeatService;
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
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private SeatController seatController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(seatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SeatResponse createMockSeat(Long id, String seatNumber, String seatClass, String status) {
        return SeatResponse.builder()
                .id(id)
                .seatNumber(seatNumber)
                .seatClass(seatClass)
                .status(status)
                .extraPrice(BigDecimal.ZERO)
                .flightId(10L)
                .isLockedByCurrentUser(false)
                .build();
    }

    @Nested
    @DisplayName("GET /seats/flight/{flightId}")
    class GetSeatsByFlightTests {

        @Test
        @DisplayName("Success - returns all seats for given flight")
        void getSeats_Success() throws Exception {
            SeatResponse seat1 = createMockSeat(1L, "1A", "FIRST", "AVAILABLE");
            SeatResponse seat2 = createMockSeat(2L, "1B", "FIRST", "OCCUPIED");

            when(seatService.getSeatsByFlight(10L)).thenReturn(List.of(seat1, seat2));

            mockMvc.perform(get("/seats/flight/{flightId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].seatNumber", is("1A")))
                    .andExpect(jsonPath("$[1].seatNumber", is("1B")));

            verify(seatService, times(1)).getSeatsByFlight(10L);
        }

        @Test
        @DisplayName("Not Found - flight does not exist returns 404")
        void getSeats_FlightNotFound_Returns404() throws Exception {
            when(seatService.getSeatsByFlight(999L))
                    .thenThrow(ApiException.notFound("Flight not found: 999"));

            mockMvc.perform(get("/seats/flight/{flightId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Flight not found: 999")));
        }
    }

    @Nested
    @DisplayName("GET /seats/flight/{flightId}/available")
    class GetAvailableSeatsTests {

        @Test
        @DisplayName("Success - returns available seats only")
        void getAvailableSeats_Success() throws Exception {
            SeatResponse seat1 = createMockSeat(1L, "1A", "FIRST", "AVAILABLE");
            when(seatService.getAvailableSeatsByFlight(10L)).thenReturn(List.of(seat1));

            mockMvc.perform(get("/seats/flight/{flightId}/available", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("AVAILABLE")));

            verify(seatService, times(1)).getAvailableSeatsByFlight(10L);
        }
    }

    @Nested
    @DisplayName("POST /seats/lock")
    class LockSeatsTests {

        @Test
        @DisplayName("Success - locks seats and returns updated seat list")
        void lockSeats_Success() throws Exception {
            SeatLockRequest req = new SeatLockRequest();
            req.setFlightId(10L);
            req.setSeatIds(List.of(1L, 2L));

            SeatResponse lockedSeat1 = createMockSeat(1L, "1A", "FIRST", "LOCKED");
            lockedSeat1.setIsLockedByCurrentUser(true);
            SeatResponse lockedSeat2 = createMockSeat(2L, "1B", "FIRST", "LOCKED");
            lockedSeat2.setIsLockedByCurrentUser(true);

            when(seatService.lockSeats(any(SeatLockRequest.class))).thenReturn(List.of(lockedSeat1, lockedSeat2));

            mockMvc.perform(post("/seats/lock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].status", is("LOCKED")));

            verify(seatService, times(1)).lockSeats(any(SeatLockRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing flightId returns 400")
        void lockSeats_MissingFlightId_Returns400() throws Exception {
            SeatLockRequest req = new SeatLockRequest();
            req.setSeatIds(List.of(1L));

            mockMvc.perform(post("/seats/lock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.flightId", notNullValue()));

            verify(seatService, never()).lockSeats(any());
        }

        @Test
        @DisplayName("Validation Error - empty seatIds returns 400")
        void lockSeats_EmptySeatIds_Returns400() throws Exception {
            SeatLockRequest req = new SeatLockRequest();
            req.setFlightId(10L);
            req.setSeatIds(Collections.emptyList());

            mockMvc.perform(post("/seats/lock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.seatIds", notNullValue()));

            verify(seatService, never()).lockSeats(any());
        }

        @Test
        @DisplayName("Conflict - seat already locked or occupied returns 409")
        void lockSeats_Conflict_Returns409() throws Exception {
            SeatLockRequest req = new SeatLockRequest();
            req.setFlightId(10L);
            req.setSeatIds(List.of(1L));

            when(seatService.lockSeats(any(SeatLockRequest.class)))
                    .thenThrow(ApiException.conflict("Seat 1A is already locked or occupied"));

            mockMvc.perform(post("/seats/lock")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Seat 1A is already locked or occupied")));
        }
    }

    @Nested
    @DisplayName("POST /seats/release")
    class ReleaseSeatsTests {

        @Test
        @DisplayName("Success - releases seats and returns 204 No Content")
        void releaseSeats_Success() throws Exception {
            SeatReleaseRequest req = new SeatReleaseRequest();
            req.setSeatIds(List.of(1L, 2L));

            mockMvc.perform(post("/seats/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(seatService, times(1)).releaseSeats(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Edge Case - empty seatIds returns 204 without calling service")
        void releaseSeats_EmptyList_Returns204() throws Exception {
            SeatReleaseRequest req = new SeatReleaseRequest();
            req.setSeatIds(Collections.emptyList());

            mockMvc.perform(post("/seats/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(seatService, never()).releaseSeats(any());
        }

        @Test
        @DisplayName("Edge Case - null seatIds returns 204 without calling service")
        void releaseSeats_NullSeatIds_Returns204() throws Exception {
            SeatReleaseRequest req = new SeatReleaseRequest();
            req.setSeatIds(null);

            mockMvc.perform(post("/seats/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            verify(seatService, never()).releaseSeats(any());
        }

        @Test
        @DisplayName("Unit Edge Case - direct call with null req")
        void directCalls_NullBranches() {
            seatController.releaseSeats(null);
            verify(seatService, never()).releaseSeats(any());

            SeatLockRequest lockReq = new SeatLockRequest();
            lockReq.setSeatIds(null);
            when(seatService.lockSeats(lockReq)).thenReturn(Collections.emptyList());
            seatController.lockSeats(lockReq);
            verify(seatService, times(1)).lockSeats(lockReq);

            when(seatService.lockSeats(null)).thenReturn(Collections.emptyList());
            seatController.lockSeats(null);
            verify(seatService, times(1)).lockSeats(null);
        }
    }
}

