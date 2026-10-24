package com.flightreservation.service;

import com.flightreservation.dto.request.SeatLockRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    /** Seats that stay LOCKED without a booking confirmed are released after this many minutes */
    private static final int LOCK_EXPIRY_MINUTES = 10;

    private final SeatRepository seatRepository;
    private final AuditLogRepository auditLogRepository;

    // ─── Read ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByFlight(Long flightId) {
        return seatRepository.findByFlightId(flightId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getAvailableSeatsByFlight(Long flightId) {
        return seatRepository.findByFlightIdAndStatus(flightId, Seat.SeatStatus.AVAILABLE)
                .stream().map(this::toResponse).toList();
    }

    // ─── Lock ────────────────────────────────────────────────────────────────

    /**
     * Atomically locks the requested seats for the current user.
     *
     * Concurrency strategy:
     *   1. Validate seats exist and belong to the right flight (plain SELECT).
     *   2. Issue a single atomic UPDATE … WHERE status = 'AVAILABLE'.
     *      PostgreSQL evaluates this under row-level locking — only one
     *      concurrent transaction can change a given row; the loser's UPDATE
     *      simply matches 0 rows for that seat.
     *   3. Compare rows-updated to rows-requested.  If they differ, at least
     *      one seat was taken between the check and the update — roll back and
     *      return 409 Conflict.
     *
     * Why no explicit SELECT … FOR UPDATE here:
     *   The atomic UPDATE WHERE is sufficient and avoids dead-lock risk that
     *   arises when two transactions acquire row locks in different seat orders.
     */
    @Transactional
    public List<SeatResponse> lockSeats(SeatLockRequest req) {
        if (req.getSeatIds() == null || req.getSeatIds().isEmpty()) {
            throw ApiException.badRequest("No seat IDs provided");
        }

        // 1. Verify seats exist and belong to the requested flight
        List<Seat> seats = seatRepository.findAllByIds(req.getSeatIds());

        if (seats.size() != req.getSeatIds().size()) {
            throw ApiException.notFound("One or more seats not found");
        }

        boolean wrongFlight = seats.stream()
                .anyMatch(s -> !s.getFlight().getId().equals(req.getFlightId()));
        if (wrongFlight) {
            throw ApiException.badRequest("Some seats do not belong to the specified flight");
        }

        // 2. Atomic UPDATE — this is the real concurrency guard.
        //    Allows locking rows that are AVAILABLE or already LOCKED by the current user.
        //    Already-LOCKED rows by other users are skipped.
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        LocalDateTime now = LocalDateTime.now();

        // Release any OTHER seats previously locked by this user on this flight that are no longer requested
        seatRepository.releaseOtherSeatsLockedByUser(
                req.getFlightId(),
                user,
                Seat.SeatStatus.LOCKED,
                Seat.SeatStatus.AVAILABLE,
                req.getSeatIds()
        );

        int updated = seatRepository.lockSeatsForUser(
                req.getSeatIds(),
                Seat.SeatStatus.AVAILABLE,
                Seat.SeatStatus.LOCKED,
                now,
                user
        );

        // 3. If fewer rows were updated than requested, another user beat us to it
        if (updated != req.getSeatIds().size()) {
            int alreadyTaken = req.getSeatIds().size() - updated;
            log.warn("Seat lock conflict for user {}: {} seat(s) already taken", user, alreadyTaken);
            throw ApiException.conflict(
                    alreadyTaken + " seat(s) were just taken by another user. Please choose different seats."
            );
        }

        log.info("User {} locked {} seat(s) on flight {}", user, updated, req.getFlightId());

        // 4. Audit
        req.getSeatIds().forEach(seatId ->
                auditLogRepository.save(AuditLog.builder()
                        .action(AuditLog.AuditAction.SEAT_LOCKED)
                        .entityType("Seat")
                        .entityId(seatId)
                        .performedBy(user)
                        .description("Seat locked for flight " + req.getFlightId()
                                + " — expires at " + now.plusMinutes(LOCK_EXPIRY_MINUTES))
                        .build())
        );

        return seatRepository.findAllByIds(req.getSeatIds()).stream().map(this::toResponse).toList();
    }

    // ─── Release / Confirm ───────────────────────────────────────────────────

    @Transactional
    public void releaseSeats(List<Long> seatIds) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        int released;
        if (isAdmin) {
            released = seatRepository.releaseSeatsAdmin(
                    seatIds, Seat.SeatStatus.LOCKED, Seat.SeatStatus.AVAILABLE);
        } else {
            released = seatRepository.releaseSeatsForUser(
                    seatIds, user, Seat.SeatStatus.LOCKED, Seat.SeatStatus.AVAILABLE);
        }
        log.info("User {} released {} seat(s)", user, released);
        seatIds.forEach(seatId ->
                auditLogRepository.save(AuditLog.builder()
                        .action(AuditLog.AuditAction.SEAT_RELEASED)
                        .entityType("Seat")
                        .entityId(seatId)
                        .performedBy(user)
                        .description("Seat manually released")
                        .build())
        );
    }

    @Transactional
    public void confirmSeats(List<Long> seatIds) {
        int confirmed = seatRepository.updateStatusByIdsAndCurrentStatus(
                seatIds, Seat.SeatStatus.LOCKED, Seat.SeatStatus.BOOKED);
        if (confirmed != seatIds.size()) {
            throw ApiException.conflict("Could not confirm all seats — lock may have expired");
        }
    }

    // ─── Scheduled lock expiry ───────────────────────────────────────────────

    /**
     * Runs every 2 minutes. Finds seats that have been LOCKED for more than
     * LOCK_EXPIRY_MINUTES without being confirmed into a booking and resets
     * them to AVAILABLE so other users can book them.
     *
     * This handles the case where a user:
     *   - Selected seats but closed the browser tab
     *   - Reached the payment page but never paid
     *   - Had a network failure mid-booking
     */
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(LOCK_EXPIRY_MINUTES);
        int released = seatRepository.releaseExpiredLocks(expiryTime);
        if (released > 0) {
            log.info("Released {} expired seat lock(s) older than {} minutes", released, LOCK_EXPIRY_MINUTES);
            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.SEAT_RELEASED)
                    .entityType("Seat")
                    .entityId(null)
                    .performedBy("system")
                    .description("Auto-released " + released + " expired seat lock(s)")
                    .build());
        }
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    public SeatResponse toResponse(Seat s) {
        String currentUser = getCurrentUserEmail();
        boolean isMine = s.getStatus() == Seat.SeatStatus.LOCKED
                && currentUser != null
                && currentUser.equalsIgnoreCase(s.getLockedBy());

        return SeatResponse.builder()
                .id(s.getId())
                .seatNumber(s.getSeatNumber())
                .seatClass(s.getSeatClass().name())
                .status(s.getStatus().name())
                .extraPrice(s.getExtraPrice())
                .flightId(s.getFlight().getId())
                .isLockedByCurrentUser(isMine)
                .build();
    }

    private String getCurrentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return null;
    }
}
