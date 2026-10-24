package com.flightreservation.controller;

import com.flightreservation.dto.request.BookingRequest;
import com.flightreservation.dto.request.CancelBookingRequest;
import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** POST /api/bookings - create a new booking */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(req));
    }

    /** GET /api/bookings/my?page=0&size=10 */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<BookingResponse>> myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getMyBookings(page, size));
    }

    /** GET /api/bookings/{reference} */
    @GetMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(bookingService.getByReference(reference));
    }

    /** PUT /api/bookings/{reference}/cancel */
    @PutMapping("/{reference}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponse> cancel(@PathVariable String reference,
                                                   @RequestBody CancelBookingRequest req) {
        return ResponseEntity.ok(bookingService.cancelBooking(reference, req));
    }
}
