package com.flightreservation.service;

import com.flightreservation.dto.request.SeatLockRequest;
import com.flightreservation.dto.response.SeatResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Flight;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.SeatRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private SeatService seatService;

    private Flight testFlight;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "passenger@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testFlight = Flight.builder().id(10L).flightNumber("AI-101").build();

        seat1 = Seat.builder()
                .id(1L)
                .flight(testFlight)
                .seatNumber("1A")
                .seatClass(Seat.SeatClass.FIRST)
                .status(Seat.SeatStatus.AVAILABLE)
                .extraPrice(BigDecimal.ZERO)
                .build();

        seat2 = Seat.builder()
                .id(2L)
                .flight(testFlight)
                .seatNumber("1B")
                .seatClass(Seat.SeatClass.FIRST)
                .status(Seat.SeatStatus.AVAILABLE)
                .extraPrice(BigDecimal.ZERO)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getSeatsByFlight - returns seats for flight")
    void getSeatsByFlight_Success() {
        when(seatRepository.findByFlightId(10L)).thenReturn(List.of(seat1, seat2));

        List<SeatResponse> result = seatService.getSeatsByFlight(10L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("1A", result.get(0).getSeatNumber());
    }

    @Test
    @DisplayName("getAvailableSeatsByFlight - returns available seats only")
    void getAvailableSeatsByFlight_Success() {
        when(seatRepository.findByFlightIdAndStatus(10L, Seat.SeatStatus.AVAILABLE)).thenReturn(List.of(seat1));

        List<SeatResponse> result = seatService.getAvailableSeatsByFlight(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1A", result.get(0).getSeatNumber());
    }

    @Test
    @DisplayName("lockSeats - successfully locks requested seats")
    void lockSeats_Success() {
        SeatLockRequest req = new SeatLockRequest();
        req.setFlightId(10L);
        req.setSeatIds(List.of(1L, 2L));

        when(seatRepository.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));
        when(seatRepository.lockSeatsForUser(eq(List.of(1L, 2L)), eq(Seat.SeatStatus.AVAILABLE),
                eq(Seat.SeatStatus.LOCKED), any(LocalDateTime.class), eq("passenger@example.com"))).thenReturn(2);

        List<SeatResponse> result = seatService.lockSeats(req);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(seatRepository, times(1)).releaseOtherSeatsLockedByUser(eq(10L), eq("passenger@example.com"),
                eq(Seat.SeatStatus.LOCKED), eq(Seat.SeatStatus.AVAILABLE), eq(List.of(1L, 2L)));
        verify(auditLogRepository, times(2)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("lockSeats - throws conflict when another user locked seats concurrently")
    void lockSeats_Conflict_ThrowsConflict() {
        SeatLockRequest req = new SeatLockRequest();
        req.setFlightId(10L);
        req.setSeatIds(List.of(1L, 2L));

        when(seatRepository.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));
        when(seatRepository.lockSeatsForUser(eq(List.of(1L, 2L)), any(), any(), any(), any())).thenReturn(1); // only 1 updated!

        assertThrows(ApiException.class, () -> seatService.lockSeats(req));
    }

    @Test
    @DisplayName("lockSeats - throws bad request when seat belongs to different flight")
    void lockSeats_WrongFlight_ThrowsBadRequest() {
        Flight otherFlight = Flight.builder().id(99L).build();
        seat2.setFlight(otherFlight);

        SeatLockRequest req = new SeatLockRequest();
        req.setFlightId(10L);
        req.setSeatIds(List.of(1L, 2L));

        when(seatRepository.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));

        assertThrows(ApiException.class, () -> seatService.lockSeats(req));
    }

    @Test
    @DisplayName("releaseSeats - user releases locked seats")
    void releaseSeats_User_Success() {
        seatService.releaseSeats(List.of(1L));

        verify(seatRepository, times(1)).releaseSeatsForUser(
                eq(List.of(1L)), eq("passenger@example.com"), eq(Seat.SeatStatus.LOCKED), eq(Seat.SeatStatus.AVAILABLE));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("confirmSeats - updates seat status to BOOKED")
    void confirmSeats_Success() {
        when(seatRepository.updateStatusByIdsAndCurrentStatus(
                List.of(1L), Seat.SeatStatus.LOCKED, Seat.SeatStatus.BOOKED)).thenReturn(1);

        seatService.confirmSeats(List.of(1L));

        verify(seatRepository, times(1)).updateStatusByIdsAndCurrentStatus(
                List.of(1L), Seat.SeatStatus.LOCKED, Seat.SeatStatus.BOOKED);
    }

    @Test
    @DisplayName("releaseExpiredLocks - releases stale locked seats")
    void releaseExpiredLocks_Success() {
        when(seatRepository.releaseExpiredLocks(any(LocalDateTime.class))).thenReturn(3);

        seatService.releaseExpiredLocks();

        verify(seatRepository, times(1)).releaseExpiredLocks(any(LocalDateTime.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("releaseExpiredLocks - zero released does not save audit log")
    void releaseExpiredLocks_ZeroReleased_NoAuditLog() {
        when(seatRepository.releaseExpiredLocks(any(LocalDateTime.class))).thenReturn(0);

        seatService.releaseExpiredLocks();

        verify(seatRepository, times(1)).releaseExpiredLocks(any(LocalDateTime.class));
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("lockSeats - null or empty seatIds throws bad request")
    void lockSeats_NullOrEmptySeatIds_ThrowsBadRequest() {
        SeatLockRequest reqNull = new SeatLockRequest();
        reqNull.setSeatIds(null);
        assertThrows(ApiException.class, () -> seatService.lockSeats(reqNull));

        SeatLockRequest reqEmpty = new SeatLockRequest();
        reqEmpty.setSeatIds(Collections.emptyList());
        assertThrows(ApiException.class, () -> seatService.lockSeats(reqEmpty));
    }

    @Test
    @DisplayName("lockSeats - seat count mismatch throws not found")
    void lockSeats_MissingSeat_ThrowsNotFound() {
        SeatLockRequest req = new SeatLockRequest();
        req.setFlightId(10L);
        req.setSeatIds(List.of(1L, 2L, 999L));

        when(seatRepository.findAllByIds(List.of(1L, 2L, 999L))).thenReturn(List.of(seat1, seat2));

        assertThrows(ApiException.class, () -> seatService.lockSeats(req));
    }

    @Test
    @DisplayName("releaseSeats - admin releases seats via admin method")
    void releaseSeats_Admin_Success() {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        seatService.releaseSeats(List.of(1L));

        verify(seatRepository, times(1)).releaseSeatsAdmin(
                eq(List.of(1L)), eq(Seat.SeatStatus.LOCKED), eq(Seat.SeatStatus.AVAILABLE));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("confirmSeats - count mismatch throws conflict")
    void confirmSeats_Mismatch_ThrowsConflict() {
        when(seatRepository.updateStatusByIdsAndCurrentStatus(
                List.of(1L, 2L), Seat.SeatStatus.LOCKED, Seat.SeatStatus.BOOKED)).thenReturn(1);

        assertThrows(ApiException.class, () -> seatService.confirmSeats(List.of(1L, 2L)));
    }

    @Test
    @DisplayName("toResponse - tests isMine flag under various authentication and lock states")
    void toResponse_Branches() {
        // 1. Locked by current user
        seat1.setStatus(Seat.SeatStatus.LOCKED);
        seat1.setLockedBy("passenger@example.com");
        SeatResponse res1 = seatService.toResponse(seat1);
        assertTrue(res1.getIsLockedByCurrentUser());

        // 2. Locked by someone else
        seat1.setLockedBy("other@example.com");
        SeatResponse res2 = seatService.toResponse(seat1);
        assertFalse(res2.getIsLockedByCurrentUser());

        // 3. Status is AVAILABLE
        seat1.setStatus(Seat.SeatStatus.AVAILABLE);
        SeatResponse res3 = seatService.toResponse(seat1);
        assertFalse(res3.getIsLockedByCurrentUser());

        // 4. Anonymous user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList()));
        seat1.setStatus(Seat.SeatStatus.LOCKED);
        seat1.setLockedBy("passenger@example.com");
        SeatResponse res4 = seatService.toResponse(seat1);
        assertFalse(res4.getIsLockedByCurrentUser());

        // 5. Unauthenticated / null auth
        SecurityContextHolder.clearContext();
        SeatResponse res5 = seatService.toResponse(seat1);
        assertFalse(res5.getIsLockedByCurrentUser());
    }
}

