package com.flightreservation.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AirlineControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirlineService airlineService;

    @InjectMocks
    private AirlineController airlineController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(airlineController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AirlineResponse createMockAirline(Long id, String code, String name) {
        return AirlineResponse.builder()
                .id(id)
                .code(code)
                .name(name)
                .country("India")
                .logoUrl("http://example.com/logo.png")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("GET /airlines")
    class GetAllAirlinesTests {

        @Test
        @DisplayName("Success - returns 200 OK with list of active airlines")
        void getAll_Success() throws Exception {
            AirlineResponse airline1 = createMockAirline(1L, "AI", "Air India");
            AirlineResponse airline2 = createMockAirline(2L, "6E", "IndiGo");

            when(airlineService.getAllActive()).thenReturn(List.of(airline1, airline2));

            mockMvc.perform(get("/airlines"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].code", is("AI")))
                    .andExpect(jsonPath("$[1].code", is("6E")));

            verify(airlineService, times(1)).getAllActive();
        }

        @Test
        @DisplayName("Success - empty list returns 200 OK")
        void getAll_Empty_Returns200() throws Exception {
            when(airlineService.getAllActive()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/airlines"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /airlines/{id}")
    class GetAirlineByIdTests {

        @Test
        @DisplayName("Success - returns airline when ID exists")
        void getById_Success() throws Exception {
            AirlineResponse airline = createMockAirline(1L, "AI", "Air India");
            when(airlineService.getById(1L)).thenReturn(airline);

            mockMvc.perform(get("/airlines/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.code", is("AI")))
                    .andExpect(jsonPath("$.name", is("Air India")));

            verify(airlineService, times(1)).getById(1L);
        }

        @Test
        @DisplayName("Failure - returns 404 when airline ID not found")
        void getById_NotFound_Returns404() throws Exception {
            when(airlineService.getById(999L)).thenThrow(ApiException.notFound("Airline not found: 999"));

            mockMvc.perform(get("/airlines/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Airline not found: 999")));

            verify(airlineService, times(1)).getById(999L);
        }
    }
}

