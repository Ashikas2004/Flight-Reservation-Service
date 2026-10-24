package com.flightreservation.service;

import com.flightreservation.dto.request.BookingRequest;
import com.flightreservation.dto.request.CancelBookingRequest;
import com.flightreservation.dto.request.PassengerRequest;
import com.flightreservation.dto.response.BookingResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.*;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private FlightRepository flightRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private SeatService seatService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private RefundService refundService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Flight testFlight;
    private Seat testSeat;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "passenger@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testUser = User.builder()
                .id(1L)
                .email("passenger@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.PASSENGER)
                .build();

        Airline testAirline = Airline.builder().id(1L).code("AI").name("Air India").build();
        Airport origin = Airport.builder().id(1L).iataCode("DEL").name("Delhi").city("Delhi").country("India").build();
        Airport dest = Airport.builder().id(2L).iataCode("BOM").name("Mumbai").city("Mumbai").country("India").build();

        testFlight = Flight.builder()
                .id(10L)
                .flightNumber("AI-101")
                .airline(testAirline)
                .originAirport(origin)
                .destinationAirport(dest)
                .departureTime(LocalDateTime.now().plusDays(2))
                .arrivalTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .basePrice(new BigDecimal("100.00"))
                .totalSeats(180)
                .availableSeats(50)
                .status(Flight.FlightStatus.SCHEDULED)
                .build();

        testSeat = Seat.builder()
                .id(101L)
                .flight(testFlight)
                .seatNumber("12A")
                .seatClass(Seat.SeatClass.ECONOMY)
                .status(Seat.SeatStatus.LOCKED)
                .extraPrice(BigDecimal.ZERO)
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .bookingReference("BK123456")
                .user(testUser)
                .flight(testFlight)
                .status(Booking.BookingStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .bookedAt(LocalDateTime.now())
                .passengers(new ArrayList<>())
                .build();

        BookingPassenger bp = BookingPassenger.builder()
                .id(1L)
                .booking(testBooking)
                .seat(testSeat)
                .firstName("John")
                .lastName("Doe")
                .passportNumber("P123456")
                .build();
        testBooking.getPassengers().add(bp);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createBooking - successfully creates booking for locked seats")
    void createBooking_Success() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        PassengerRequest pr = new PassengerRequest();
        pr.setSeatId(101L);
        pr.setFirstName("John");
        pr.setLastName("Doe");
        pr.setPassportNumber("P123456");
        pr.setDateOfBirth("1990-01-01");
        pr.setNationality("Indian");
        req.setPassengers(List.of(pr));

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(flightRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testFlight));
        when(bookingRepository.findPendingBookingsByUserAndFlight(10L, 1L)).thenReturn(Collections.emptyList());
        when(seatRepository.findAllByIds(List.of(101L))).thenReturn(List.of(testSeat));
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(req);

        assertNotNull(resp);
        assertEquals("PENDING", resp.getStatus());
        verify(flightRepository, times(1)).save(testFlight);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - throws conflict when seats are not locked")
    void createBooking_UnlockedSeats_ThrowsConflict() {
        testSeat.setStatus(Seat.SeatStatus.AVAILABLE); // not locked!

        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        PassengerRequest pr = new PassengerRequest();
        pr.setSeatId(101L);
        req.setPassengers(List.of(pr));

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(flightRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testFlight));
        when(seatRepository.findAllByIds(List.of(101L))).thenReturn(List.of(testSeat));

        assertThrows(ApiException.class, () -> bookingService.createBooking(req));
    }

    @Test
    @DisplayName("getMyBookings - returns user bookings paginated")
    void getMyBookings_Success() {
        Page<Booking> page = new PageImpl<>(List.of(testBooking));
        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(bookingRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<BookingResponse> result = bookingService.getMyBookings(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("BK123456", result.getContent().get(0).getBookingReference());
    }

    @Test
    @DisplayName("getByReference - success for booking owner")
    void getByReference_Success() {
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        BookingResponse result = bookingService.getByReference("BK123456");

        assertNotNull(result);
        assertEquals("BK123456", result.getBookingReference());
    }

    @Test
    @DisplayName("getByReference - throws forbidden when accessed by non-owner and non-admin")
    void getByReference_NonOwner_ThrowsForbidden() {
        testUser.setEmail("other@example.com");
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> bookingService.getByReference("BK123456"));
    }

    @Test
    @DisplayName("confirmBooking - confirms pending booking and seats")
    void confirmBooking_Success() {
        when(bookingRepository.findByIdWithPassengers(1L)).thenReturn(Optional.of(testBooking));

        BookingResponse result = bookingService.confirmBooking(1L);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        verify(seatService, times(1)).confirmSeats(List.of(101L));
        verify(bookingRepository, times(1)).save(testBooking);
    }

    @Test
    @DisplayName("cancelBooking - cancels booking and triggers automatic refund if paid")
    void cancelBooking_WithPayment_TriggersRefund() {
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        Payment payment = Payment.builder()
                .id(10L)
                .booking(testBooking)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(Payment.PaymentStatus.SUCCESS)
                .build();
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        CancelBookingRequest req = new CancelBookingRequest();
        req.setReason("Change of travel plans");

        BookingResponse result = bookingService.cancelBooking("BK123456", req);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());
        verify(refundService, times(1)).processAutomaticRefund(eq(testBooking), eq(payment), eq("Change of travel plans"), anyString());
    }

    @Test
    @DisplayName("createBooking - flight not scheduled throws bad request")
    void createBooking_FlightNotScheduled_ThrowsBadRequest() {
        testFlight.setStatus(Flight.FlightStatus.CANCELLED);
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(flightRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testFlight));

        assertThrows(ApiException.class, () -> bookingService.createBooking(req));
    }

    @Test
    @DisplayName("createBooking - cancels existing pending bookings on same flight")
    void createBooking_CancelsExistingPending() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        PassengerRequest pr = new PassengerRequest();
        pr.setSeatId(101L);
        pr.setFirstName("John");
        pr.setLastName("Doe");
        req.setPassengers(List.of(pr));

        Booking oldPending = Booking.builder()
                .id(99L)
                .bookingReference("OLD99")
                .flight(testFlight)
                .user(testUser)
                .status(Booking.BookingStatus.PENDING)
                .passengers(List.of(BookingPassenger.builder().seat(testSeat).build()))
                .build();

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(flightRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testFlight));
        when(bookingRepository.findPendingBookingsByUserAndFlight(10L, 1L)).thenReturn(List.of(oldPending));
        when(seatRepository.findAllByIds(List.of(101L))).thenReturn(List.of(testSeat));
        when(bookingRepository.existsByBookingReference(anyString())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse resp = bookingService.createBooking(req);

        assertNotNull(resp);
        assertEquals(Booking.BookingStatus.CANCELLED, oldPending.getStatus());
        verify(bookingRepository, atLeast(2)).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking - seat missing throws not found")
    void createBooking_MissingSeat_ThrowsNotFound() {
        BookingRequest req = new BookingRequest();
        req.setFlightId(10L);
        PassengerRequest pr = new PassengerRequest();
        pr.setSeatId(101L);
        req.setPassengers(List.of(pr));

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(flightRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(testFlight));
        when(bookingRepository.findPendingBookingsByUserAndFlight(10L, 1L)).thenReturn(Collections.emptyList());
        when(seatRepository.findAllByIds(List.of(101L))).thenReturn(Collections.emptyList());

        assertThrows(ApiException.class, () -> bookingService.createBooking(req));
    }

    @Test
    @DisplayName("getByReference - finds by numeric ID fallback")
    void getByReference_NumericIdFallback() {
        when(bookingRepository.findByReferenceWithDetails("1")).thenReturn(Optional.empty());
        when(bookingRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(testBooking));

        BookingResponse result = bookingService.getByReference("1");
        assertNotNull(result);
        assertEquals("BK123456", result.getBookingReference());
    }

    @Test
    @DisplayName("getByReference - admin can view other user's booking")
    void getByReference_AdminAccess_Success() {
        testUser.setEmail("other@example.com");
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        BookingResponse result = bookingService.getByReference("BK123456");
        assertNotNull(result);
    }

    @Test
    @DisplayName("confirmBooking - non-pending booking throws bad request")
    void confirmBooking_NonPending_ThrowsBadRequest() {
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithPassengers(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> bookingService.confirmBooking(1L));
    }

    @Test
    @DisplayName("cancelBooking - non-owner non-admin throws forbidden")
    void cancelBooking_NonOwner_ThrowsForbidden() {
        testUser.setEmail("other@example.com");
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        CancelBookingRequest req = new CancelBookingRequest();
        assertThrows(ApiException.class, () -> bookingService.cancelBooking("BK123456", req));
    }

    @Test
    @DisplayName("cancelBooking - already cancelled or refunded throws bad request")
    void cancelBooking_AlreadyCancelled_ThrowsBadRequest() {
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        CancelBookingRequest req = new CancelBookingRequest();
        assertThrows(ApiException.class, () -> bookingService.cancelBooking("BK123456", req));

        testBooking.setStatus(Booking.BookingStatus.REFUNDED);
        assertThrows(ApiException.class, () -> bookingService.cancelBooking("BK123456", req));
    }

    @Test
    @DisplayName("cancelBooking - completed journey cannot be cancelled")
    void cancelBooking_CompletedFlight_ThrowsBadRequest() {
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        testFlight.setStatus(Flight.FlightStatus.COMPLETED);
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> bookingService.cancelBooking("BK123456", new CancelBookingRequest()));

        verify(bookingRepository, never()).save(testBooking);
        verifyNoInteractions(refundService);
    }

    @Test
    @DisplayName("cancelBooking - no payment or non-success payment does not refund")
    void cancelBooking_NoPayment_NoRefund() {
        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        CancelBookingRequest req = new CancelBookingRequest();
        req.setReason("Cancel without payment");

        BookingResponse result = bookingService.cancelBooking("BK123456", req);
        assertEquals("CANCELLED", result.getStatus());
        verify(refundService, never()).processAutomaticRefund(any(), any(), any(), any());
    }

    @Test
    @DisplayName("toResponse - includes refund info when refund exists and handles null seat passenger")
    void toResponse_WithRefundAndNullSeat() {
        testBooking.getPassengers().get(0).setSeat(null);

        Refund refund = Refund.builder()
                .id(50L)
                .booking(testBooking)
                .refundAmount(new BigDecimal("100.00"))
                .currency("USD")
                .status(Refund.RefundStatus.PROCESSED)
                .refundTransactionId("REF-123")
                .processedAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findByReferenceWithDetails("BK123456")).thenReturn(Optional.of(testBooking));
        when(refundRepository.findByBookingId(1L)).thenReturn(Optional.of(refund));

        BookingResponse resp = bookingService.getByReference("BK123456");

        assertNotNull(resp);
        assertNotNull(resp.getRefund());
        assertEquals("REF-123", resp.getRefund().getRefundTransactionId());
        assertNull(resp.getPassengers().get(0).getSeatNumber());
        assertEquals("SCHEDULED", resp.getFlight().getStatus());
    }
}
