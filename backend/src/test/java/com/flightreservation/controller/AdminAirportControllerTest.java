package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.AirportRequest;
import com.flightreservation.dto.response.AirportResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.AirportService;
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

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAirportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirportService airportService;

    @InjectMocks
    private AdminAirportController adminAirportController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAirportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AirportResponse createMockAirport(Long id, String iataCode, String name, boolean active) {
        return AirportResponse.builder()
                .id(id)
                .iataCode(iataCode)
                .name(name)
                .city("City")
                .country("Country")
                .active(active)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/airports")
    class GetAllAirportsTests {

        @Test
        @DisplayName("Success - returns all airports")
        void getAll_Success() throws Exception {
            AirportResponse a1 = createMockAirport(1L, "JFK", "JFK Airport", true);
            AirportResponse a2 = createMockAirport(2L, "LAX", "LAX Airport", false);

            when(airportService.getAll()).thenReturn(List.of(a1, a2));

            mockMvc.perform(get("/admin/airports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].iataCode", is("JFK")))
                    .andExpect(jsonPath("$[1].iataCode", is("LAX")));

            verify(airportService, times(1)).getAll();
        }
    }

    @Nested
    @DisplayName("POST /admin/airports")
    class CreateAirportTests {

        @Test
        @DisplayName("Success - creates airport and returns 201 Created")
        void create_Success() throws Exception {
            AirportRequest req = new AirportRequest();
            req.setIataCode("DEL");
            req.setName("Delhi Airport");
            req.setCity("Delhi");
            req.setCountry("India");

            AirportResponse resp = createMockAirport(1L, "DEL", "Delhi Airport", true);
            when(airportService.create(any(AirportRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/admin/airports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.iataCode", is("DEL")));

            verify(airportService, times(1)).create(any(AirportRequest.class));
        }

        @Test
        @DisplayName("Validation Error - IATA code not 3 chars returns 400")
        void create_InvalidIataLength_Returns400() throws Exception {
            AirportRequest req = new AirportRequest();
            req.setIataCode("DELHI");
            req.setName("Delhi Airport");
            req.setCity("Delhi");
            req.setCountry("India");

            mockMvc.perform(post("/admin/airports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.iataCode", is("IATA code must be exactly 3 characters")));

            verify(airportService, never()).create(any());
        }

        @Test
        @DisplayName("Conflict - duplicate IATA code returns 409 Conflict")
        void create_DuplicateIata_Returns409() throws Exception {
            AirportRequest req = new AirportRequest();
            req.setIataCode("DEL");
            req.setName("Delhi Airport");
            req.setCity("Delhi");
            req.setCountry("India");

            when(airportService.create(any(AirportRequest.class)))
                    .thenThrow(ApiException.conflict("Airport with IATA code DEL already exists"));

            mockMvc.perform(post("/admin/airports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Airport with IATA code DEL already exists")));
        }
    }

    @Nested
    @DisplayName("PUT /admin/airports/{id}")
    class UpdateAirportTests {

        @Test
        @DisplayName("Success - updates airport and returns 200 OK")
        void update_Success() throws Exception {
            AirportRequest req = new AirportRequest();
            req.setIataCode("DEL");
            req.setName("Indira Gandhi International Airport");
            req.setCity("New Delhi");
            req.setCountry("India");

            AirportResponse resp = createMockAirport(1L, "DEL", "Indira Gandhi International Airport", true);
            when(airportService.update(eq(1L), any(AirportRequest.class))).thenReturn(resp);

            mockMvc.perform(put("/admin/airports/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Indira Gandhi International Airport")));

            verify(airportService, times(1)).update(eq(1L), any(AirportRequest.class));
        }

        @Test
        @DisplayName("Not Found - airport ID not found returns 404")
        void update_NotFound_Returns404() throws Exception {
            AirportRequest req = new AirportRequest();
            req.setIataCode("DEL");
            req.setName("Indira Gandhi International Airport");
            req.setCity("New Delhi");
            req.setCountry("India");

            when(airportService.update(eq(999L), any(AirportRequest.class)))
                    .thenThrow(ApiException.notFound("Airport not found: 999"));

            mockMvc.perform(put("/admin/airports/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Airport not found: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/airports/{id}/active")
    class ToggleActiveTests {

        @Test
        @DisplayName("Success - toggles active flag and returns 204 No Content")
        void toggleActive_Success() throws Exception {
            Map<String, Boolean> body = Map.of("active", false);

            mockMvc.perform(patch("/admin/airports/{id}/active", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNoContent());

            verify(airportService, times(1)).toggleActive(1L, false);
        }
    }
}

