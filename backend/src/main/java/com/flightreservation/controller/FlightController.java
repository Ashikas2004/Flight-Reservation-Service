package com.flightreservation.controller;

import com.flightreservation.dto.request.FlightSearchRequest;
import com.flightreservation.dto.response.FlightResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    /** GET /api/flights/search?origin=JFK&destination=LAX&departureDate=2026-12-01&passengers=2 */
    @GetMapping("/search")
    public ResponseEntity<PageResponse<FlightResponse>> search(@Valid FlightSearchRequest req) {
        return ResponseEntity.ok(flightService.searchFlights(req));
    }

    /** GET /api/flights/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }
}
