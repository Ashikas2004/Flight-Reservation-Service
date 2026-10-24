package com.flightreservation.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AirportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AirportService airportService;

    @InjectMocks
    private AirportController airportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(airportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AirportResponse createMockAirport(Long id, String iataCode, String name, String city, String country) {
        return AirportResponse.builder()
                .id(id)
                .iataCode(iataCode)
                .name(name)
                .city(city)
                .country(country)
                .timezone("Asia/Kolkata")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("GET /airports")
    class GetAllAirportsTests {

        @Test
        @DisplayName("Success - returns 200 OK with list of airports")
        void getAll_Success() throws Exception {
            AirportResponse airport1 = createMockAirport(1L, "DEL", "Indira Gandhi Int Airport", "Delhi", "India");
            AirportResponse airport2 = createMockAirport(2L, "BOM", "Chhatrapati Shivaji Int Airport", "Mumbai", "India");

            when(airportService.getAll()).thenReturn(List.of(airport1, airport2));

            mockMvc.perform(get("/airports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].iataCode", is("DEL")))
                    .andExpect(jsonPath("$[1].iataCode", is("BOM")));

            verify(airportService, times(1)).getAll();
        }

        @Test
        @DisplayName("Success - returns empty list when no airports exist")
        void getAll_Empty_Returns200() throws Exception {
            when(airportService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/airports"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /airports/{id}")
    class GetAirportByIdTests {

        @Test
        @DisplayName("Success - returns airport when ID exists")
        void getById_Success() throws Exception {
            AirportResponse airport = createMockAirport(1L, "DEL", "Indira Gandhi Int Airport", "Delhi", "India");
            when(airportService.getById(1L)).thenReturn(airport);

            mockMvc.perform(get("/airports/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.iataCode", is("DEL")))
                    .andExpect(jsonPath("$.city", is("Delhi")));

            verify(airportService, times(1)).getById(1L);
        }

        @Test
        @DisplayName("Failure - returns 404 when airport ID not found")
        void getById_NotFound_Returns404() throws Exception {
            when(airportService.getById(999L)).thenThrow(ApiException.notFound("Airport not found: 999"));

            mockMvc.perform(get("/airports/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", is("Airport not found: 999")));

            verify(airportService, times(1)).getById(999L);
        }
    }

    @Nested
    @DisplayName("GET /airports/search")
    class SearchAirportsTests {

        @Test
        @DisplayName("Success - returns matched airports")
        void search_Success() throws Exception {
            AirportResponse airport = createMockAirport(1L, "DEL", "Indira Gandhi Int Airport", "Delhi", "India");
            when(airportService.search("Delhi")).thenReturn(List.of(airport));

            mockMvc.perform(get("/airports/search").param("q", "Delhi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].iataCode", is("DEL")));

            verify(airportService, times(1)).search("Delhi");
        }

        @Test
        @DisplayName("Failure - missing 'q' query parameter returns 400 Bad Request")
        void search_MissingQueryParam_Returns400() throws Exception {
            mockMvc.perform(get("/airports/search"))
                    .andExpect(status().isBadRequest());

            verify(airportService, never()).search(anyString());
        }
    }
}

