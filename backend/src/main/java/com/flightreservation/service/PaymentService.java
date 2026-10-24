package com.flightreservation.service;

import com.flightreservation.dto.request.PaymentRequest;
import com.flightreservation.dto.response.PaymentResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Booking;
import com.flightreservation.entity.BookingPassenger;
import com.flightreservation.entity.Payment;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogRepository auditLogRepository;
    private final MockPaymentGateway paymentGateway;
    private final SeatService seatService;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Booking booking = bookingRepository.findByIdWithPassengers(req.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + req.getBookingId()));

        if (!booking.getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Access denied to booking");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw ApiException.badRequest("Booking is not in PENDING state: " + booking.getStatus());
        }

        if (paymentRepository.existsByBookingId(booking.getId())) {
            throw ApiException.conflict("Payment already initiated for this booking");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .currency("INR")
                .paymentMethod(Payment.PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase()))
                .status(Payment.PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.PAYMENT_INITIATED)
                .entityType("Payment")
                .entityId(payment.getId())
                .performedBy(email)
                .description("Payment initiated for booking: " + booking.getBookingReference())
                .build());

        // Call mock gateway
        MockPaymentGateway.GatewayResult result = paymentGateway.charge(
                req.getPaymentMethod(),
                booking.getTotalAmount(),
                "INR",
                Map.of("bookingRef", booking.getBookingReference(), "user", email)
        );

        if (result.success()) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setTransactionId(result.transactionId());
            payment.setGatewayResponse(result.gatewayResponse());
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Confirm booking — update status and confirm seats
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            List<Long> seatIds = booking.getPassengers().stream()
                    .map(bp -> bp.getSeat().getId()).toList();
            seatService.confirmSeats(seatIds);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.PAYMENT_SUCCESS)
                    .entityType("Payment")
                    .entityId(payment.getId())
                    .performedBy(email)
                    .description("Payment successful: " + result.transactionId())
                    .build());

            log.info("Payment successful for booking {}: txn={}", booking.getBookingReference(), result.transactionId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailedReason(result.errorMessage());
            payment.setGatewayResponse(result.errorCode());
            paymentRepository.save(payment);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.PAYMENT_FAILED)
                    .entityType("Payment")
                    .entityId(payment.getId())
                    .performedBy(email)
                    .description("Payment failed: " + result.errorMessage())
                    .build());

            log.warn("Payment failed for booking {}: {}", booking.getBookingReference(), result.errorMessage());
        }

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Payment payment = paymentRepository.findByBookingIdWithDetails(bookingId)
                .orElseThrow(() -> ApiException.notFound("Payment not found for booking: " + bookingId));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !payment.getBooking().getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Access denied");
        }

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .bookingId(p.getBooking().getId())
                .bookingReference(p.getBooking().getBookingReference())
                .transactionId(p.getTransactionId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod().name())
                .status(p.getStatus().name())
                .paidAt(p.getPaidAt())
                .failedReason(p.getFailedReason())
                .build();
    }
}
