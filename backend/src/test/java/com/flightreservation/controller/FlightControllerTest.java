package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.FlightSearchRequest;
import com.flightreservation.dto.response.FlightResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.FlightService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlightService flightService;

    @InjectMocks
    private FlightController flightController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flightController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private FlightResponse createMockFlightResponse(Long id, String flightNumber) {
        return FlightResponse.builder()
                .id(id)
                .flightNumber(flightNumber)
                .airline(FlightResponse.AirlineInfo.builder()
                        .id(1L)
                        .code("AI")
                        .name("Air India")
                        .logoUrl("http://example.com/logo.png")
                        .build())
                .originAirport(FlightResponse.AirportInfo.builder()
                        .id(1L)
                        .iataCode("JFK")
                        .name("John F Kennedy")
                        .city("New York")
                        .country("USA")
                        .build())
                .destinationAirport(FlightResponse.AirportInfo.builder()
                        .id(2L)
                        .iataCode("LAX")
                        .name("Los Angeles International")
                        .city("Los Angeles")
                        .country("USA")
                        .build())
                .departureTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 14, 0))
                .basePrice(new BigDecimal("250.00"))
                .totalSeats(180)
                .availableSeats(50)
                .status("SCHEDULED")
                .durationMinutes(240)
                .build();
    }

    @Nested
    @DisplayName("GET /flights/search")
    class SearchFlightsTests {

        @Test
        @DisplayName("Success - returns 200 OK with paginated flights")
        void search_Success() throws Exception {
            FlightResponse flight = createMockFlightResponse(1L, "AI-101");
            PageResponse<FlightResponse> pageResponse = PageResponse.<FlightResponse>builder()
                    .content(List.of(flight))
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(flightService.searchFlights(any(FlightSearchRequest.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/flights/search")
                            .param("origin", "JFK")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01")
                            .param("passengers", "2")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].flightNumber", is("AI-101")))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.pageNumber", is(0)));

            verify(flightService, times(1)).searchFlights(any(FlightSearchRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing origin returns 400 Bad Request")
        void search_MissingOrigin_Returns400() throws Exception {
            mockMvc.perform(get("/flights/search")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.origin", notNullValue()));

            verify(flightService, never()).searchFlights(any());
        }

        @Test
        @DisplayName("Validation Error - invalid origin format (not 3 chars) returns 400")
        void search_InvalidOriginLength_Returns400() throws Exception {
            mockMvc.perform(get("/flights/search")
                            .param("origin", "NEWYORK")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.origin", is("Origin must be a 3-letter IATA code")));

            verify(flightService, never()).searchFlights(any());
        }

        @Test
        @DisplayName("Validation Error - missing departureDate returns 400")
        void search_MissingDepartureDate_Returns400() throws Exception {
            mockMvc.perform(get("/flights/search")
                            .param("origin", "JFK")
                            .param("destination", "LAX"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.departureDate", notNullValue()));

            verify(flightService, never()).searchFlights(any());
        }

        @Test
        @DisplayName("Validation Error - passengers less than 1 returns 400")
        void search_PassengersLessThanOne_Returns400() throws Exception {
            mockMvc.perform(get("/flights/search")
                            .param("origin", "JFK")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01")
                            .param("passengers", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.passengers", is("At least 1 passenger required")));

            verify(flightService, never()).searchFlights(any());
        }

        @Test
        @DisplayName("Success - empty results returns 200 OK with empty content")
        void search_EmptyResults_Returns200() throws Exception {
            PageResponse<FlightResponse> emptyPage = PageResponse.<FlightResponse>builder()
                    .content(Collections.emptyList())
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();

            when(flightService.searchFlights(any(FlightSearchRequest.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/flights/search")
                            .param("origin", "JFK")
                            .param("destination", "LAX")
                            .param("departureDate", "2026-12-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /flights/{id}")
    class GetFlightByIdTests {

        @Test
        @DisplayName("Success - valid flight ID returns 200 OK")
        void getById_Success() throws Exception {
            FlightResponse flight = createMockFlightResponse(100L, "AI-202");
            when(flightService.getFlightById(100L)).thenReturn(flight);

            mockMvc.perform(get("/flights/{id}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(100)))
                    .andExpect(jsonPath("$.flightNumber", is("AI-202")))
                    .andExpect(jsonPath("$.status", is("SCHEDULED")));

            verify(flightService, times(1)).getFlightById(100L);
        }

        @Test
        @DisplayName("Failure - flight ID not found returns 404 Not Found")
        void getById_NotFound_Returns404() throws Exception {
            when(flightService.getFlightById(999L))
                    .thenThrow(ApiException.notFound("Flight not found with ID: 999"));

            mockMvc.perform(get("/flights/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Flight not found with ID: 999")));

            verify(flightService, times(1)).getFlightById(999L);
        }

        @Test
        @DisplayName("Edge Case - invalid data type in path variable returns 400/500")
        void getById_InvalidDataType_ReturnsError() throws Exception {
            mockMvc.perform(get("/flights/{id}", "not-a-number"))
                    .andExpect(status().is5xxServerError());
        }
    }
}

