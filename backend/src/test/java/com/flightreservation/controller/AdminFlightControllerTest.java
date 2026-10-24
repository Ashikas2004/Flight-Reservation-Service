package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flightreservation.dto.request.FlightRequest;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminFlightControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FlightService flightService;

    @InjectMocks
    private AdminFlightController adminFlightController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminFlightController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private FlightRequest createValidFlightRequest() {
        FlightRequest req = new FlightRequest();
        req.setFlightNumber("AI-303");
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.now().plusDays(2));
        req.setArrivalTime(LocalDateTime.now().plusDays(2).plusHours(3));
        req.setBasePrice(new BigDecimal("350.00"));
        req.setFirstClassSeats(8);
        req.setBusinessSeats(24);
        req.setEconomySeats(120);
        return req;
    }

    private FlightResponse createMockFlightResponse(Long id, String flightNumber) {
        return FlightResponse.builder()
                .id(id)
                .flightNumber(flightNumber)
                .status("SCHEDULED")
                .basePrice(new BigDecimal("350.00"))
                .totalSeats(152)
                .availableSeats(152)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/flights")
    class GetAllFlightsTests {

        @Test
        @DisplayName("Success - returns paginated flights for admin")
        void getAll_Success() throws Exception {
            FlightResponse f = createMockFlightResponse(1L, "AI-303");
            PageResponse<FlightResponse> page = PageResponse.<FlightResponse>builder()
                    .content(List.of(f))
                    .pageNumber(0)
                    .pageSize(50)
                    .totalElements(1)
                    .totalPages(1)
                    .last(true)
                    .build();

            when(flightService.getAllFlights(0, 50)).thenReturn(page);

            mockMvc.perform(get("/admin/flights")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].flightNumber", is("AI-303")));

            verify(flightService, times(1)).getAllFlights(0, 50);
        }
    }

    @Nested
    @DisplayName("POST /admin/flights")
    class CreateFlightTests {

        @Test
        @DisplayName("Success - creates flight and returns 201 Created")
        void create_Success() throws Exception {
            FlightRequest req = createValidFlightRequest();
            FlightResponse resp = createMockFlightResponse(1L, "AI-303");

            when(flightService.createFlight(any(FlightRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/admin/flights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.flightNumber", is("AI-303")));

            verify(flightService, times(1)).createFlight(any(FlightRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing required fields returns 400 Bad Request")
        void create_MissingFields_Returns400() throws Exception {
            FlightRequest req = new FlightRequest(); // all fields null

            mockMvc.perform(post("/admin/flights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.flightNumber", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.airlineId", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.originAirportId", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.destinationAirportId", notNullValue()));

            verify(flightService, never()).createFlight(any());
        }

        @Test
        @DisplayName("Validation Error - basePrice less than 0.01 returns 400")
        void create_InvalidBasePrice_Returns400() throws Exception {
            FlightRequest req = createValidFlightRequest();
            req.setBasePrice(new BigDecimal("0.00"));

            mockMvc.perform(post("/admin/flights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.basePrice", is("Price must be greater than 0")));
        }

        @Test
        @DisplayName("Conflict - flight with same number on same departure date returns 409")
        void create_DuplicateFlight_Returns409() throws Exception {
            FlightRequest req = createValidFlightRequest();

            when(flightService.createFlight(any(FlightRequest.class)))
                    .thenThrow(ApiException.conflict("Flight number already exists for the scheduled date"));

            mockMvc.perform(post("/admin/flights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Flight number already exists for the scheduled date")));
        }
    }

    @Nested
    @DisplayName("PUT /admin/flights/{id}")
    class UpdateFlightTests {

        @Test
        @DisplayName("Success - updates flight and returns 200 OK")
        void update_Success() throws Exception {
            FlightRequest req = createValidFlightRequest();
            FlightResponse resp = createMockFlightResponse(1L, "AI-303-UPDATED");

            when(flightService.updateFlight(eq(1L), any(FlightRequest.class))).thenReturn(resp);

            mockMvc.perform(put("/admin/flights/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.flightNumber", is("AI-303-UPDATED")));

            verify(flightService, times(1)).updateFlight(eq(1L), any(FlightRequest.class));
        }

        @Test
        @DisplayName("Not Found - flight ID not found returns 404")
        void update_NotFound_Returns404() throws Exception {
            FlightRequest req = createValidFlightRequest();

            when(flightService.updateFlight(eq(999L), any(FlightRequest.class)))
                    .thenThrow(ApiException.notFound("Flight not found: 999"));

            mockMvc.perform(put("/admin/flights/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Flight not found: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/flights/{id}/status")
    class UpdateFlightStatusTests {

        @Test
        @DisplayName("Success - updates flight status and returns 204 No Content")
        void updateStatus_Success() throws Exception {
            Map<String, String> body = Map.of("status", "DELAYED");

            mockMvc.perform(patch("/admin/flights/{id}/status", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNoContent());

            verify(flightService, times(1)).updateFlightStatus(1L, "DELAYED");
        }

        @Test
        @DisplayName("Failure - invalid status throws ApiException and returns 400")
        void updateStatus_InvalidStatus_Returns400() throws Exception {
            Map<String, String> body = Map.of("status", "UNKNOWN_STATUS");

            doThrow(ApiException.badRequest("Invalid flight status: UNKNOWN_STATUS"))
                    .when(flightService).updateFlightStatus(1L, "UNKNOWN_STATUS");

            mockMvc.perform(patch("/admin/flights/{id}/status", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Invalid flight status: UNKNOWN_STATUS")));
        }
    }
}

