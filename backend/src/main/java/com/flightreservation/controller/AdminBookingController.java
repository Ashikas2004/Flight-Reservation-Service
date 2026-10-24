package com.flightreservation.controller;

import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    /** GET /api/admin/bookings/{reference} - admin view of any booking */
    @GetMapping("/{reference}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String reference) {
        return ResponseEntity.ok(bookingService.getByReference(reference));
    }
}
