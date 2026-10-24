package com.flightreservation.controller;

import com.flightreservation.dto.request.FlightRequest;
import com.flightreservation.dto.response.FlightResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/flights")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlightController {

    private final FlightService flightService;

    /** GET /api/admin/flights?page=0&size=50 — list ALL flights for admin */
    @GetMapping
    public ResponseEntity<PageResponse<FlightResponse>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(flightService.getAllFlights(page, size));
    }

    @PostMapping
    public ResponseEntity<FlightResponse> create(@Valid @RequestBody FlightRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody FlightRequest req) {
        return ResponseEntity.ok(flightService.updateFlight(id, req));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                              @RequestBody Map<String, String> body) {
        flightService.updateFlightStatus(id, body.get("status"));
        return ResponseEntity.noContent().build();
    }
}
