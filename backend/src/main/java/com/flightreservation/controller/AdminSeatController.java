package com.flightreservation.controller;

import com.flightreservation.dto.request.SeatRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.SeatRepository;
import com.flightreservation.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/seats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSeatController {

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;
    private final SeatService seatService;

    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<SeatResponse>> getSeatsByFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(seatService.getSeatsByFlight(flightId));
    }

    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody SeatRequest req) {
        var flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> ApiException.notFound("Flight not found: " + req.getFlightId()));

        Seat seat = Seat.builder()
                .flight(flight)
                .seatNumber(req.getSeatNumber().toUpperCase())
                .seatClass(Seat.SeatClass.valueOf(req.getSeatClass().toUpperCase()))
                .status(Seat.SeatStatus.AVAILABLE)
                .extraPrice(req.getExtraPrice())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.toResponse(seatRepository.save(seat)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SeatResponse> updateSeatStatus(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Seat not found: " + id));
        seat.setStatus(Seat.SeatStatus.valueOf(body.get("status").toUpperCase()));
        return ResponseEntity.ok(seatService.toResponse(seatRepository.save(seat)));
    }
}
