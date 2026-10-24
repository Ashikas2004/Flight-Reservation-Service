package com.flightreservation.controller;

import com.flightreservation.dto.request.AirlineRequest;
import com.flightreservation.dto.request.AirlineUpdateRequest;
import com.flightreservation.dto.response.AirlineResponse;
import com.flightreservation.service.AirlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/airlines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAirlineController {

    private final AirlineService airlineService;

    @GetMapping
    public ResponseEntity<List<AirlineResponse>> getAll() {
        return ResponseEntity.ok(airlineService.getAll());
    }

    @PostMapping
    public ResponseEntity<AirlineResponse> create(@Valid @RequestBody AirlineRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airlineService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirlineResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody AirlineUpdateRequest req) {
        return ResponseEntity.ok(airlineService.update(id, req));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id,
                                              @RequestBody Map<String, Boolean> body) {
        airlineService.toggleActive(id, body.get("active"));
        return ResponseEntity.noContent().build();
    }
}
