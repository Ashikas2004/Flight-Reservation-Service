package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.SeatRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.entity.Flight;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.SeatRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminSeatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private AdminSeatController adminSeatController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminSeatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SeatResponse createMockSeatResponse(Long id, String seatNumber, String seatClass, String status) {
        return SeatResponse.builder()
                .id(id)
                .seatNumber(seatNumber)
                .seatClass(seatClass)
                .status(status)
                .extraPrice(BigDecimal.ZERO)
                .flightId(10L)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/seats/flight/{flightId}")
    class GetSeatsByFlightTests {

        @Test
        @DisplayName("Success - returns seats for flight")
        void getSeats_Success() throws Exception {
            SeatResponse seat = createMockSeatResponse(1L, "1A", "FIRST", "AVAILABLE");
            when(seatService.getSeatsByFlight(10L)).thenReturn(List.of(seat));

            mockMvc.perform(get("/admin/seats/flight/{flightId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].seatNumber", is("1A")));

            verify(seatService, times(1)).getSeatsByFlight(10L);
        }
    }

    @Nested
    @DisplayName("POST /admin/seats")
    class CreateSeatTests {

        @Test
        @DisplayName("Success - creates new seat and returns 201 Created")
        void createSeat_Success() throws Exception {
            SeatRequest req = new SeatRequest();
            req.setFlightId(10L);
            req.setSeatNumber("1A");
            req.setSeatClass("FIRST");
            req.setExtraPrice(new BigDecimal("50.00"));

            Flight mockFlight = Flight.builder().id(10L).build();
            when(flightRepository.findById(10L)).thenReturn(Optional.of(mockFlight));

            Seat savedSeat = Seat.builder()
                    .id(1L)
                    .flight(mockFlight)
                    .seatNumber("1A")
                    .seatClass(Seat.SeatClass.FIRST)
                    .status(Seat.SeatStatus.AVAILABLE)
                    .extraPrice(new BigDecimal("50.00"))
                    .build();

            when(seatRepository.save(any(Seat.class))).thenReturn(savedSeat);
            when(seatService.toResponse(savedSeat)).thenReturn(createMockSeatResponse(1L, "1A", "FIRST", "AVAILABLE"));

            mockMvc.perform(post("/admin/seats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.seatNumber", is("1A")));

            verify(seatRepository, times(1)).save(any(Seat.class));
        }

        @Test
        @DisplayName("Not Found - flight ID not found returns 404")
        void createSeat_FlightNotFound_Returns404() throws Exception {
            SeatRequest req = new SeatRequest();
            req.setFlightId(999L);
            req.setSeatNumber("1A");
            req.setSeatClass("FIRST");

            when(flightRepository.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/admin/seats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Flight not found: 999")));

            verify(seatRepository, never()).save(any());
        }

        @Test
        @DisplayName("Validation Error - missing required fields returns 400 Bad Request")
        void createSeat_MissingFields_Returns400() throws Exception {
            SeatRequest req = new SeatRequest();

            mockMvc.perform(post("/admin/seats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.flightId", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.seatNumber", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.seatClass", notNullValue()));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/seats/{id}/status")
    class UpdateSeatStatusTests {

        @Test
        @DisplayName("Success - updates seat status and returns 200 OK")
        void updateSeatStatus_Success() throws Exception {
            Seat existingSeat = Seat.builder()
                    .id(1L)
                    .seatNumber("1A")
                    .seatClass(Seat.SeatClass.FIRST)
                    .status(Seat.SeatStatus.AVAILABLE)
                    .build();

            when(seatRepository.findById(1L)).thenReturn(Optional.of(existingSeat));
            when(seatRepository.save(existingSeat)).thenReturn(existingSeat);
            when(seatService.toResponse(existingSeat))
                    .thenReturn(createMockSeatResponse(1L, "1A", "FIRST", "BOOKED"));

            Map<String, String> body = Map.of("status", "BOOKED");

            mockMvc.perform(patch("/admin/seats/{id}/status", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("BOOKED")));

            verify(seatRepository, times(1)).save(existingSeat);
        }

        @Test
        @DisplayName("Not Found - seat ID not found returns 404")
        void updateSeatStatus_NotFound_Returns404() throws Exception {
            when(seatRepository.findById(999L)).thenReturn(Optional.empty());

            Map<String, String> body = Map.of("status", "BOOKED");

            mockMvc.perform(patch("/admin/seats/{id}/status", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Seat not found: 999")));
        }
    }
}

