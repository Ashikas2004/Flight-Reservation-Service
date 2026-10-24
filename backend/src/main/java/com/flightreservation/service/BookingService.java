package com.flightreservation.service;

import com.flightreservation.dto.request.BookingRequest;
import com.flightreservation.dto.request.CancelBookingRequest;
import com.flightreservation.dto.request.PassengerRequest;
import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Booking;
import com.flightreservation.entity.BookingPassenger;
import com.flightreservation.entity.Flight;
import com.flightreservation.entity.Seat;
import com.flightreservation.entity.User;
import com.flightreservation.exception.ApiException;
import com.flightreservation.entity.Payment;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.PaymentRepository;
import com.flightreservation.repository.RefundRepository;
import com.flightreservation.repository.SeatRepository;
import com.flightreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SeatService seatService;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RefundService refundService;

    @Transactional
    public BookingResponse createBooking(BookingRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        Flight flight = flightRepository.findByIdWithDetails(req.getFlightId())
                .orElseThrow(() -> ApiException.notFound("Flight not found: " + req.getFlightId()));

        if (flight.getStatus() != Flight.FlightStatus.SCHEDULED) {
            throw ApiException.badRequest("Flight is not available for booking: " + flight.getStatus());
        }

        // Cancel any previous PENDING bookings for this user on the same flight (e.g. if user went back to change seats)
        List<Booking> existingPending = bookingRepository.findPendingBookingsByUserAndFlight(flight.getId(), user.getId());
        for (Booking oldPending : existingPending) {
            oldPending.setStatus(Booking.BookingStatus.CANCELLED);
            oldPending.setCancelledAt(LocalDateTime.now());
            oldPending.setCancellationReason("Superseded by new booking selection");
            bookingRepository.save(oldPending);
            flight.setAvailableSeats(flight.getAvailableSeats() + oldPending.getPassengers().size());
            log.info("Cancelled previous pending booking {} superseded by new booking for user {}",
                    oldPending.getBookingReference(), email);
        }

        // Collect and validate seats
        List<Long> seatIds = req.getPassengers().stream()
                .map(PassengerRequest::getSeatId).toList();

        List<Seat> seats = seatRepository.findAllByIds(seatIds);
        if (seats.size() != seatIds.size()) {
            throw ApiException.notFound("One or more seats not found");
        }

        boolean unlockedSeats = seats.stream()
                .anyMatch(s -> s.getStatus() != Seat.SeatStatus.LOCKED);
        if (unlockedSeats) {
            throw ApiException.conflict("All seats must be locked before booking. Please lock seats first.");
        }

        // Calculate total
        BigDecimal total = flight.getBasePrice().multiply(BigDecimal.valueOf(seatIds.size()));
        for (Seat seat : seats) {
            total = total.add(seat.getExtraPrice());
        }

        // Build booking
        String reference = generateReference();
        Booking booking = Booking.builder()
                .bookingReference(reference)
                .user(user)
                .flight(flight)
                .status(Booking.BookingStatus.PENDING)
                .totalAmount(total)
                .bookedAt(LocalDateTime.now())
                .build();

        // Build passengers
        List<BookingPassenger> passengers = new ArrayList<>();
        for (PassengerRequest pr : req.getPassengers()) {
            Seat seat = seats.stream().filter(s -> s.getId().equals(pr.getSeatId()))
                    .findFirst().orElseThrow();
            passengers.add(BookingPassenger.builder()
                    .booking(booking)
                    .seat(seat)
                    .firstName(pr.getFirstName())
                    .lastName(pr.getLastName())
                    .passportNumber(pr.getPassportNumber())
                    .dateOfBirth(pr.getDateOfBirth())
                    .nationality(pr.getNationality())
                    .build());
        }
        booking.setPassengers(passengers);

        bookingRepository.save(booking);

        // Update flight available seats
        flight.setAvailableSeats(flight.getAvailableSeats() - seatIds.size());
        flightRepository.save(flight);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.BOOKING_CREATED)
                .entityType("Booking")
                .entityId(booking.getId())
                .performedBy(email)
                .description("Booking created: " + reference + " for flight " + flight.getFlightNumber())
                .build());

        log.info("Booking created: {} by {}", reference, email);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(int page, int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(bookingRepository.findByUserId(user.getId(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public BookingResponse getByReference(String reference) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking booking = bookingRepository.findByReferenceWithDetails(reference)
                .or(() -> {
                    try {
                        Long id = Long.parseLong(reference);
                        return bookingRepository.findByIdWithDetails(id);
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + reference));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !booking.getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Access denied to booking: " + reference);
        }
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithPassengers(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + bookingId));
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw ApiException.badRequest("Booking cannot be confirmed, current status: " + booking.getStatus());
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        // Confirm all seats
        List<Long> seatIds = booking.getPassengers().stream()
                .map(bp -> bp.getSeat().getId()).toList();
        seatService.confirmSeats(seatIds);

        bookingRepository.save(booking);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.BOOKING_CONFIRMED)
                .entityType("Booking")
                .entityId(booking.getId())
                .performedBy(booking.getUser().getEmail())
                .description("Booking confirmed: " + booking.getBookingReference())
                .build());

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String reference, CancelBookingRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Booking booking = bookingRepository.findByReferenceWithDetails(reference)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + reference));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !booking.getUser().getEmail().equals(email)) {
            throw ApiException.forbidden("Access denied");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED ||
            booking.getStatus() == Booking.BookingStatus.REFUNDED) {
            throw ApiException.badRequest("Booking is already " + booking.getStatus());
        }

        Flight flight = booking.getFlight();
        if (flight.getStatus() == Flight.FlightStatus.COMPLETED ||
                !flight.getArrivalTime().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("This journey is completed and can no longer be cancelled");
        }
        if (!flight.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("This flight has already departed and can no longer be cancelled");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(req.getReason());

        // Release seats
        List<Long> seatIds = booking.getPassengers().stream()
                .map(bp -> bp.getSeat().getId()).toList();
        seatRepository.updateStatusByIdsAndCurrentStatus(
                seatIds, Seat.SeatStatus.BOOKED, Seat.SeatStatus.AVAILABLE);
        seatRepository.updateStatusByIdsAndCurrentStatus(
                seatIds, Seat.SeatStatus.LOCKED, Seat.SeatStatus.AVAILABLE);

        // Restore flight available seats
        flight.setAvailableSeats(flight.getAvailableSeats() + seatIds.size());
        flightRepository.save(flight);

        bookingRepository.save(booking);

        auditLogRepository.save(AuditLog.builder()
                .action(AuditLog.AuditAction.BOOKING_CANCELLED)
                .entityType("Booking")
                .entityId(booking.getId())
                .performedBy(email)
                .description("Booking cancelled: " + reference + ". Reason: " + req.getReason())
                .build());

        // Process automatic refund if payment was already made
        Optional<Payment> paymentOpt = paymentRepository.findByBookingId(booking.getId());
        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == Payment.PaymentStatus.SUCCESS) {
            refundService.processAutomaticRefund(booking, paymentOpt.get(), req.getReason(), email);
        }

        log.info("Booking cancelled: {}", reference);
        return toResponse(booking);
    }

    private String generateReference() {
        String ref;
        do {
            ref = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (bookingRepository.existsByBookingReference(ref));
        return ref;
    }

    private BookingResponse toResponse(Booking b) {
        Flight f = b.getFlight();
        List<BookingResponse.PassengerInfo> passengers = b.getPassengers().stream()
                .map(bp -> BookingResponse.PassengerInfo.builder()
                        .id(bp.getId())
                        .firstName(bp.getFirstName())
                        .lastName(bp.getLastName())
                        .seatNumber(bp.getSeat() != null ? bp.getSeat().getSeatNumber() : null)
                        .seatClass(bp.getSeat() != null ? bp.getSeat().getSeatClass().name() : null)
                        .passportNumber(bp.getPassportNumber())
                        .build())
                .toList();

        BookingResponse resp = BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .status(b.getStatus().name())
                .totalAmount(b.getTotalAmount())
                .bookedAt(b.getBookedAt())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .flight(BookingResponse.FlightSummary.builder()
                        .id(f.getId())
                        .flightNumber(f.getFlightNumber())
                        .airlineName(f.getAirline().getName())
                        .originIata(f.getOriginAirport().getIataCode())
                        .destinationIata(f.getDestinationAirport().getIataCode())
                        .originCity(f.getOriginAirport().getCity())
                        .destinationCity(f.getDestinationAirport().getCity())
                        .departureTime(f.getDepartureTime())
                        .arrivalTime(f.getArrivalTime())
                        .status(f.getStatus().name())
                        .build())
                .passengers(passengers)
                .build();

        refundRepository.findByBookingId(b.getId()).ifPresent(r -> {
            resp.setRefund(BookingResponse.RefundInfo.builder()
                    .id(r.getId())
                    .refundAmount(r.getRefundAmount())
                    .currency(r.getCurrency())
                    .status(r.getStatus().name())
                    .refundTransactionId(r.getRefundTransactionId())
                    .processedAt(r.getProcessedAt())
                    .build());
        });

        return resp;
    }
}
