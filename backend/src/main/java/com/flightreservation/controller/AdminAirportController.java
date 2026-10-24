package com.flightreservation.controller;

import com.flightreservation.dto.request.AirportRequest;
import com.flightreservation.dto.response.AirportResponse;
import com.flightreservation.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/airports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAirportController {

    private final AirportService airportService;

    @GetMapping
    public ResponseEntity<List<AirportResponse>> getAll() {
        return ResponseEntity.ok(airportService.getAll());
    }

    @PostMapping
    public ResponseEntity<AirportResponse> create(@Valid @RequestBody AirportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirportResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody AirportRequest req) {
        return ResponseEntity.ok(airportService.update(id, req));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body) {
        airportService.toggleActive(id, body.get("active"));
        return ResponseEntity.noContent().build();
    }
}
