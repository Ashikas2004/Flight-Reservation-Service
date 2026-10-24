package com.flightreservation.service;

import com.flightreservation.dto.request.RefundRequest;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.dto.response.RefundResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Booking;
import com.flightreservation.entity.Payment;
import com.flightreservation.entity.Refund;
import com.flightreservation.entity.User;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.PaymentRepository;
import com.flightreservation.repository.RefundRepository;
import com.flightreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final MockPaymentGateway paymentGateway;

    @Transactional
    public RefundResponse requestRefund(RefundRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + req.getBookingId()));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !booking.getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Access denied");
        }

        if (booking.getStatus() != Booking.BookingStatus.CANCELLED) {
            throw ApiException.badRequest("Booking must be cancelled before requesting a refund");
        }

        if (refundRepository.existsByBookingId(booking.getId())) {
            throw ApiException.conflict("Refund already requested for this booking");
        }

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> ApiException.notFound("No payment found for this booking"));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw ApiException.badRequest("Cannot refund a payment that was not successful");
        }

        Refund refund = Refund.builder()
                .booking(booking)
                .payment(payment)
                .refundAmount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(Refund.RefundStatus.PENDING)
                .reason(req.getReason())
                .build();

        refund = refundRepository.save(refund);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.REFUND_REQUESTED)
                .entityType("Refund")
                .entityId(refund.getId())
                .performedBy(email)
                .description("Refund requested for booking: " + booking.getBookingReference())
                .build());

        // Process through mock gateway
        MockPaymentGateway.GatewayResult result = paymentGateway.refund(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        if (result.success()) {
            refund.setStatus(Refund.RefundStatus.PROCESSED);
            refund.setRefundTransactionId(result.transactionId());
            refund.setProcessedAt(LocalDateTime.now());

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            booking.setStatus(Booking.BookingStatus.REFUNDED);
            bookingRepository.save(booking);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.REFUND_PROCESSED)
                    .entityType("Refund")
                    .entityId(refund.getId())
                    .performedBy(email)
                    .description("Refund processed: " + result.transactionId())
                    .build());

            log.info("Refund processed for booking {}: {}", booking.getBookingReference(), result.transactionId());
        } else {
            refund.setStatus(Refund.RefundStatus.FAILED);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.REFUND_FAILED)
                    .entityType("Refund")
                    .entityId(refund.getId())
                    .performedBy(email)
                    .description("Refund failed: " + result.errorMessage())
                    .build());

            log.warn("Refund failed for booking {}", booking.getBookingReference());
        }

        return toResponse(refundRepository.save(refund));
    }

    @Transactional
    public Refund processAutomaticRefund(Booking booking, Payment payment, String reason, String performedByEmail) {
        if (refundRepository.existsByBookingId(booking.getId())) {
            log.warn("Refund already exists for booking {}", booking.getId());
            return refundRepository.findByBookingId(booking.getId()).orElse(null);
        }

        Refund refund = Refund.builder()
                .booking(booking)
                .payment(payment)
                .refundAmount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(Refund.RefundStatus.PENDING)
                .reason(reason != null && !reason.isBlank() ? reason : "Automatic refund upon booking cancellation")
                .build();

        refund = refundRepository.save(refund);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.REFUND_REQUESTED)
                .entityType("Refund")
                .entityId(refund.getId())
                .performedBy(performedByEmail)
                .description("Automatic refund initiated for cancelled booking: " + booking.getBookingReference())
                .build());

        // Process through mock gateway
        MockPaymentGateway.GatewayResult result = paymentGateway.refund(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getCurrency()
        );

        if (result.success()) {
            refund.setStatus(Refund.RefundStatus.PROCESSED);
            refund.setRefundTransactionId(result.transactionId());
            refund.setProcessedAt(LocalDateTime.now());

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            booking.setStatus(Booking.BookingStatus.REFUNDED);
            bookingRepository.save(booking);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.REFUND_PROCESSED)
                    .entityType("Refund")
                    .entityId(refund.getId())
                    .performedBy(performedByEmail)
                    .description("Automatic refund processed: " + result.transactionId())
                    .build());

            log.info("Automatic refund processed for booking {}: {}", booking.getBookingReference(), result.transactionId());
        } else {
            refund.setStatus(Refund.RefundStatus.FAILED);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.REFUND_FAILED)
                    .entityType("Refund")
                    .entityId(refund.getId())
                    .performedBy(performedByEmail)
                    .description("Automatic refund failed: " + result.errorMessage())
                    .build());

            log.warn("Automatic refund failed for booking {}", booking.getBookingReference());
        }

        return refundRepository.save(refund);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getMyRefunds(int page, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(refundRepository.findByUserId(user.getId(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public RefundResponse getByBookingId(Long bookingId) {
        Refund refund = refundRepository.findByBookingId(bookingId)
                .orElseThrow(() -> ApiException.notFound("Refund not found for booking: " + bookingId));
        return toResponse(refund);
    }

    private RefundResponse toResponse(Refund r) {
        return RefundResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .bookingReference(r.getBooking().getBookingReference())
                .refundAmount(r.getRefundAmount())
                .currency(r.getCurrency())
                .status(r.getStatus().name())
                .reason(r.getReason())
                .refundTransactionId(r.getRefundTransactionId())
                .processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
