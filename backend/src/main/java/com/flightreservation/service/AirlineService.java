package com.flightreservation.service;

import com.flightreservation.dto.request.AirlineRequest;
import com.flightreservation.dto.request.AirlineUpdateRequest;
import com.flightreservation.dto.response.AirlineResponse;
import com.flightreservation.entity.Airline;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AirlineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirlineService {

    private final AirlineRepository airlineRepository;

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAllActive() {
        return airlineRepository.findAllByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AirlineResponse> getAll() {
        return airlineRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AirlineResponse getById(Long id) {
        return toResponse(airlineRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airline not found: " + id)));
    }

    @Transactional
    public AirlineResponse create(AirlineRequest req) {
        if (airlineRepository.existsByCode(req.getCode().toUpperCase())) {
            throw ApiException.conflict("Airline code already exists: " + req.getCode());
        }
        Airline airline = Airline.builder()
                .code(req.getCode().toUpperCase())
                .name(req.getName())
                .country(req.getCountry())
                .logoUrl(req.getLogoUrl())
                .active(true)
                .build();
        return toResponse(airlineRepository.save(airline));
    }

    @Transactional
    public AirlineResponse update(Long id, AirlineUpdateRequest req) {
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airline not found: " + id));
        airline.setName(req.getName());
        airline.setCountry(req.getCountry());
        airline.setLogoUrl(req.getLogoUrl());
        return toResponse(airlineRepository.save(airline));
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        Airline airline = airlineRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Airline not found: " + id));
        airline.setActive(active);
        airlineRepository.save(airline);
    }

    private AirlineResponse toResponse(Airline a) {
        return AirlineResponse.builder()
                .id(a.getId()).code(a.getCode()).name(a.getName())
                .country(a.getCountry()).logoUrl(a.getLogoUrl()).active(a.getActive())
                .build();
    }
}
