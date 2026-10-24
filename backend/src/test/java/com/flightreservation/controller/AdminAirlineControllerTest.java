package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.AirlineRequest;
import com.flightreservation.dto.request.AirlineUpdateRequest;
import com.flightreservation.dto.response.AirlineResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.AirlineService;
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
class AdminAirlineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirlineService airlineService;

    @InjectMocks
    private AdminAirlineController adminAirlineController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAirlineController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AirlineResponse createMockAirline(Long id, String code, String name, boolean active) {
        return AirlineResponse.builder()
                .id(id)
                .code(code)
                .name(name)
                .country("India")
                .logoUrl("http://example.com/logo.png")
                .active(active)
                .build();
    }

    @Nested
    @DisplayName("GET /admin/airlines")
    class GetAllAirlinesTests {

        @Test
        @DisplayName("Success - returns all airlines including inactive ones")
        void getAll_Success() throws Exception {
            AirlineResponse a1 = createMockAirline(1L, "AI", "Air India", true);
            AirlineResponse a2 = createMockAirline(2L, "SJ", "SpiceJet", false);

            when(airlineService.getAll()).thenReturn(List.of(a1, a2));

            mockMvc.perform(get("/admin/airlines"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].active", is(true)))
                    .andExpect(jsonPath("$[1].active", is(false)));

            verify(airlineService, times(1)).getAll();
        }
    }

    @Nested
    @DisplayName("POST /admin/airlines")
    class CreateAirlineTests {

        @Test
        @DisplayName("Success - creates new airline and returns 201 Created")
        void create_Success() throws Exception {
            AirlineRequest req = new AirlineRequest();
            req.setCode("AI");
            req.setName("Air India");
            req.setCountry("India");
            req.setLogoUrl("http://example.com/ai.png");

            AirlineResponse resp = createMockAirline(1L, "AI", "Air India", true);
            when(airlineService.create(any(AirlineRequest.class))).thenReturn(resp);

            mockMvc.perform(post("/admin/airlines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.code", is("AI")))
                    .andExpect(jsonPath("$.name", is("Air India")));

            verify(airlineService, times(1)).create(any(AirlineRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing required fields returns 400 Bad Request")
        void create_MissingFields_Returns400() throws Exception {
            AirlineRequest req = new AirlineRequest();

            mockMvc.perform(post("/admin/airlines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.code", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.name", notNullValue()));

            verify(airlineService, never()).create(any());
        }

        @Test
        @DisplayName("Conflict - duplicate airline code returns 409 Conflict")
        void create_DuplicateCode_Returns409() throws Exception {
            AirlineRequest req = new AirlineRequest();
            req.setCode("AI");
            req.setName("Air India");

            when(airlineService.create(any(AirlineRequest.class)))
                    .thenThrow(ApiException.conflict("Airline with code AI already exists"));

            mockMvc.perform(post("/admin/airlines")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Airline with code AI already exists")));
        }
    }

    @Nested
    @DisplayName("PUT /admin/airlines/{id}")
    class UpdateAirlineTests {

        @Test
        @DisplayName("Success - updates airline and returns 200 OK")
        void update_Success() throws Exception {
            AirlineUpdateRequest req = new AirlineUpdateRequest();
            req.setName("Air India Express");
            req.setCountry("India");

            AirlineResponse resp = createMockAirline(1L, "AI", "Air India Express", true);
            when(airlineService.update(eq(1L), any(AirlineUpdateRequest.class))).thenReturn(resp);

            mockMvc.perform(put("/admin/airlines/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Air India Express")));

            verify(airlineService, times(1)).update(eq(1L), any(AirlineUpdateRequest.class));
        }

        @Test
        @DisplayName("Validation Error - blank name returns 400")
        void update_BlankName_Returns400() throws Exception {
            AirlineUpdateRequest req = new AirlineUpdateRequest();
            req.setName("");

            mockMvc.perform(put("/admin/airlines/{id}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
        }

        @Test
        @DisplayName("Not Found - airline ID not found returns 404")
        void update_NotFound_Returns404() throws Exception {
            AirlineUpdateRequest req = new AirlineUpdateRequest();
            req.setName("Updated Name");

            when(airlineService.update(eq(999L), any(AirlineUpdateRequest.class)))
                    .thenThrow(ApiException.notFound("Airline not found: 999"));

            mockMvc.perform(put("/admin/airlines/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Airline not found: 999")));
        }
    }

    @Nested
    @DisplayName("PATCH /admin/airlines/{id}/active")
    class ToggleActiveTests {

        @Test
        @DisplayName("Success - toggles active status and returns 204 No Content")
        void toggleActive_Success() throws Exception {
            Map<String, Boolean> body = Map.of("active", false);

            mockMvc.perform(patch("/admin/airlines/{id}/active", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNoContent());

            verify(airlineService, times(1)).toggleActive(1L, false);
        }

        @Test
        @DisplayName("Not Found - airline ID not found returns 404")
        void toggleActive_NotFound_Returns404() throws Exception {
            Map<String, Boolean> body = Map.of("active", false);
            doThrow(ApiException.notFound("Airline not found: 999"))
                    .when(airlineService).toggleActive(999L, false);

            mockMvc.perform(patch("/admin/airlines/{id}/active", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }
    }
}

