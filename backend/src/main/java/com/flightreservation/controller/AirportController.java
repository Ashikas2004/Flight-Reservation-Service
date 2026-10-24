package com.flightreservation.controller;

import com.flightreservation.dto.response.AirportResponse;
import com.flightreservation.service.AirportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    @GetMapping
    public ResponseEntity<List<AirportResponse>> getAll() {
        return ResponseEntity.ok(airportService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(airportService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AirportResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(airportService.search(q));
    }
}
