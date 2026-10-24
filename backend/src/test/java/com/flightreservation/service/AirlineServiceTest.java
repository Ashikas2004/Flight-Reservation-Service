package com.flightreservation.service;

import com.flightreservation.dto.request.AirlineRequest;
import com.flightreservation.dto.request.AirlineUpdateRequest;
import com.flightreservation.dto.response.AirlineResponse;
import com.flightreservation.entity.Airline;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AirlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceTest {

    @Mock
    private AirlineRepository airlineRepository;

    @InjectMocks
    private AirlineService airlineService;

    private Airline testAirline;

    @BeforeEach
    void setUp() {
        testAirline = Airline.builder()
                .id(1L)
                .code("AI")
                .name("Air India")
                .country("India")
                .logoUrl("http://example.com/logo.png")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getAllActive - returns active airlines")
    void getAllActive_ReturnsActiveAirlines() {
        when(airlineRepository.findAllByActiveTrue()).thenReturn(List.of(testAirline));

        List<AirlineResponse> result = airlineService.getAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AI", result.get(0).getCode());
        verify(airlineRepository, times(1)).findAllByActiveTrue();
    }

    @Test
    @DisplayName("getAll - returns all airlines")
    void getAll_ReturnsAllAirlines() {
        when(airlineRepository.findAll()).thenReturn(List.of(testAirline));

        List<AirlineResponse> result = airlineService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AI", result.get(0).getCode());
        verify(airlineRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getById - success when airline exists")
    void getById_Success() {
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));

        AirlineResponse result = airlineService.getById(1L);

        assertNotNull(result);
        assertEquals("AI", result.getCode());
        assertEquals("Air India", result.getName());
    }

    @Test
    @DisplayName("getById - throws not found when airline does not exist")
    void getById_NotFound_ThrowsException() {
        when(airlineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> airlineService.getById(999L));
    }

    @Test
    @DisplayName("create - successfully creates airline when code is unique")
    void create_Success() {
        AirlineRequest req = new AirlineRequest();
        req.setCode("AI");
        req.setName("Air India");
        req.setCountry("India");
        req.setLogoUrl("http://example.com/logo.png");

        when(airlineRepository.existsByCode("AI")).thenReturn(false);
        when(airlineRepository.save(any(Airline.class))).thenReturn(testAirline);

        AirlineResponse result = airlineService.create(req);

        assertNotNull(result);
        assertEquals("AI", result.getCode());
        verify(airlineRepository, times(1)).save(any(Airline.class));
    }

    @Test
    @DisplayName("create - throws conflict when airline code already exists")
    void create_DuplicateCode_ThrowsConflict() {
        AirlineRequest req = new AirlineRequest();
        req.setCode("AI");

        when(airlineRepository.existsByCode("AI")).thenReturn(true);

        assertThrows(ApiException.class, () -> airlineService.create(req));
        verify(airlineRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - successfully updates airline")
    void update_Success() {
        AirlineUpdateRequest req = new AirlineUpdateRequest();
        req.setName("Air India Express");
        req.setCountry("India");
        req.setLogoUrl("http://example.com/new.png");

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airlineRepository.save(any(Airline.class))).thenReturn(testAirline);

        AirlineResponse result = airlineService.update(1L, req);

        assertNotNull(result);
        assertEquals("Air India Express", testAirline.getName());
        verify(airlineRepository, times(1)).save(testAirline);
    }

    @Test
    @DisplayName("update - throws not found when airline does not exist")
    void update_NotFound_ThrowsException() {
        AirlineUpdateRequest req = new AirlineUpdateRequest();
        when(airlineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> airlineService.update(999L, req));
    }

    @Test
    @DisplayName("toggleActive - updates active flag")
    void toggleActive_Success() {
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));

        airlineService.toggleActive(1L, false);

        assertFalse(testAirline.getActive());
        verify(airlineRepository, times(1)).save(testAirline);
    }

    @Test
    @DisplayName("toggleActive - throws not found when ID does not exist")
    void toggleActive_NotFound_ThrowsException() {
        when(airlineRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> airlineService.toggleActive(999L, false));
    }
}

