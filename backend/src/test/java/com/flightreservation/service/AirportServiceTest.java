package com.flightreservation.service;

import com.flightreservation.dto.request.AirportRequest;
import com.flightreservation.dto.response.AirportResponse;
import com.flightreservation.entity.Airport;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AirportRepository;
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
class AirportServiceTest {

    @Mock
    private AirportRepository airportRepository;

    @InjectMocks
    private AirportService airportService;

    private Airport testAirport;

    @BeforeEach
    void setUp() {
        testAirport = Airport.builder()
                .id(1L)
                .iataCode("DEL")
                .name("Indira Gandhi International Airport")
                .city("Delhi")
                .country("India")
                .timezone("Asia/Kolkata")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getAll - returns active airports")
    void getAll_ReturnsActiveAirports() {
        when(airportRepository.findAllByActiveTrue()).thenReturn(List.of(testAirport));

        List<AirportResponse> result = airportService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DEL", result.get(0).getIataCode());
    }

    @Test
    @DisplayName("getById - success when airport exists")
    void getById_Success() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(testAirport));

        AirportResponse result = airportService.getById(1L);

        assertNotNull(result);
        assertEquals("DEL", result.getIataCode());
        assertEquals("Delhi", result.getCity());
    }

    @Test
    @DisplayName("getById - throws not found when airport does not exist")
    void getById_NotFound_ThrowsException() {
        when(airportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> airportService.getById(999L));
    }

    @Test
    @DisplayName("search - searches by city or airport name")
    void search_ReturnsMatchingAirports() {
        when(airportRepository.findByCityContainingIgnoreCaseOrNameContainingIgnoreCase("Delhi", "Delhi"))
                .thenReturn(List.of(testAirport));

        List<AirportResponse> result = airportService.search("Delhi");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("DEL", result.get(0).getIataCode());
    }

    @Test
    @DisplayName("create - successfully creates airport when IATA code is unique")
    void create_Success() {
        AirportRequest req = new AirportRequest();
        req.setIataCode("DEL");
        req.setName("Delhi Airport");
        req.setCity("Delhi");
        req.setCountry("India");
        req.setTimezone("Asia/Kolkata");

        when(airportRepository.existsByIataCode("DEL")).thenReturn(false);
        when(airportRepository.save(any(Airport.class))).thenReturn(testAirport);

        AirportResponse result = airportService.create(req);

        assertNotNull(result);
        assertEquals("DEL", result.getIataCode());
        verify(airportRepository, times(1)).save(any(Airport.class));
    }

    @Test
    @DisplayName("create - throws conflict when IATA code exists")
    void create_DuplicateIata_ThrowsConflict() {
        AirportRequest req = new AirportRequest();
        req.setIataCode("DEL");

        when(airportRepository.existsByIataCode("DEL")).thenReturn(true);

        assertThrows(ApiException.class, () -> airportService.create(req));
        verify(airportRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - successfully updates airport")
    void update_Success() {
        AirportRequest req = new AirportRequest();
        req.setName("New Delhi Int Airport");
        req.setCity("New Delhi");
        req.setCountry("India");
        req.setTimezone("Asia/Kolkata");

        when(airportRepository.findById(1L)).thenReturn(Optional.of(testAirport));
        when(airportRepository.save(any(Airport.class))).thenReturn(testAirport);

        AirportResponse result = airportService.update(1L, req);

        assertNotNull(result);
        assertEquals("New Delhi Int Airport", testAirport.getName());
    }

    @Test
    @DisplayName("update - throws not found when airport does not exist")
    void update_NotFound_ThrowsException() {
        AirportRequest req = new AirportRequest();
        when(airportRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> airportService.update(999L, req));
    }

    @Test
    @DisplayName("toggleActive - updates active flag")
    void toggleActive_Success() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(testAirport));

        airportService.toggleActive(1L, false);

        assertFalse(testAirport.getActive());
        verify(airportRepository, times(1)).save(testAirport);
    }
}

