package com.flightreservation.controller;

import com.flightreservation.dto.response.AirlineResponse;
import com.flightreservation.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/airlines")
@RequiredArgsConstructor
public class AirlineController {

    private final AirlineService airlineService;

    @GetMapping
    public ResponseEntity<List<AirlineResponse>> getAll() {
        return ResponseEntity.ok(airlineService.getAllActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirlineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(airlineService.getById(id));
    }
}
