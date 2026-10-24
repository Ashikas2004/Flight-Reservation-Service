package com.flightreservation.controller;

import com.flightreservation.dto.request.SeatLockRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /** GET /api/seats/flight/{flightId} — all seats for a flight */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getSeatsByFlight(flightId));
    }

    /** GET /api/seats/flight/{flightId}/available */
    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeatsByFlight(flightId));
    }

    /** POST /api/seats/lock — atomically lock seats before booking */
    @PostMapping("/lock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SeatResponse>> lockSeats(@Valid @RequestBody SeatLockRequest req) {
        if (req != null && req.getSeatIds() != null) {
            req.setSeatIds(req.getSeatIds().stream().map(Number::longValue).toList());
        }
        return ResponseEntity.ok(seatService.lockSeats(req));
    }

    /**
     * POST /api/seats/release — release previously locked seats back to AVAILABLE.
     * Called when the user goes back from the passenger details page to re-pick seats.
     * Body: { "seatIds": [1, 2, 3] }
     */
    @PostMapping("/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> releaseSeats(@RequestBody com.flightreservation.dto.request.SeatReleaseRequest req) {
        if (req != null && req.getSeatIds() != null && !req.getSeatIds().isEmpty()) {
            List<Long> seatIds = req.getSeatIds().stream()
                    .map(Number::longValue)
                    .toList();
            seatService.releaseSeats(seatIds);
        }
        return ResponseEntity.noContent().build();
    }
}
