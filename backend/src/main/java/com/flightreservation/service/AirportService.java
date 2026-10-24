package com.flightreservation.service;

import com.flightreservation.dto.request.AirportRequest;
import com.flightreservation.dto.response.AirportResponse;
import com.flightreservation.entity.Airport;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;

    @Transactional(readOnly = true)
    public List<AirportResponse> getAll() {
        return airportRepository.findAllByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AirportResponse getById(Long id) {
        return toResponse(airportRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airport not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<AirportResponse> search(String query) {
        return airportRepository
                .findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(query, query)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AirportResponse create(AirportRequest req) {
        if (airportRepository.existsByIataCode(req.getIataCode().toUpperCase())) {
            throw ApiException.conflict("IATA code already exists: " + req.getIataCode());
        }
        Airport airport = Airport.builder()
                .iataCode(req.getIataCode().toUpperCase())
                .name(req.getName())
                .city(req.getCity())
                .country(req.getCountry())
                .timezone(req.getTimezone())
                .active(true)
                .build();
        return toResponse(airportRepository.save(airport));
    }

    @Transactional
    public AirportResponse update(Long id, AirportRequest req) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airport not found: " + id));
        airport.setName(req.getName());
        airport.setCity(req.getCity());
        airport.setCountry(req.getCountry());
        airport.setTimezone(req.getTimezone());
        return toResponse(airportRepository.save(airport));
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airport not found: " + id));
        airport.setActive(active);
        airportRepository.save(airport);
    }

    private AirportResponse toResponse(Airport a) {
        return AirportResponse.builder()
                .id(a.getId()).iataCode(a.getIataCode()).name(a.getName())
                .city(a.getCity()).country(a.getCountry())
                .timezone(a.getTimezone()).active(a.getActive())
                .build();
    }
}
