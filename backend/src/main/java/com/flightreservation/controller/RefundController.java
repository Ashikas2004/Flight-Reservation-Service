package com.flightreservation.controller;

import com.flightreservation.dto.request.RefundRequest;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.dto.response.RefundResponse;
import com.flightreservation.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /** POST /api/refunds - request a refund for a cancelled booking */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RefundResponse> requestRefund(@Valid @RequestBody RefundRequest req) {
        return ResponseEntity.ok(refundService.requestRefund(req));
    }

    /** GET /api/refunds/my */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<RefundResponse>> myRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(refundService.getMyRefunds(page, size));
    }

    /** GET /api/refunds/booking/{bookingId} */
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RefundResponse> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(refundService.getByBookingId(bookingId));
    }
}
