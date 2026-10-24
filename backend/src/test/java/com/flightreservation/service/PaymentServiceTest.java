package com.flightreservation.service;

import com.flightreservation.dto.request.PaymentRequest;
import com.flightreservation.dto.response.PaymentResponse;
import com.flightreservation.entity.*;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.PaymentRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private MockPaymentGateway paymentGateway;
    @Mock
    private SeatService seatService;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Booking testBooking;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testUser = User.builder().id(1L).email("user@example.com").build();

        testBooking = Booking.builder()
                .id(100L)
                .bookingReference("BK-999")
                .user(testUser)
                .status(Booking.BookingStatus.PENDING)
                .totalAmount(new BigDecimal("250.00"))
                .passengers(new ArrayList<>())
                .build();

        Seat seat = Seat.builder().id(50L).status(Seat.SeatStatus.LOCKED).build();
        BookingPassenger bp = BookingPassenger.builder().id(1L).seat(seat).build();
        testBooking.getPassengers().add(bp);

        testPayment = Payment.builder()
                .id(1L)
                .booking(testBooking)
                .amount(new BigDecimal("250.00"))
                .currency("INR")
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .status(Payment.PaymentStatus.SUCCESS)
                .transactionId("TXN-12345")
                .paidAt(LocalDateTime.now())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("processPayment - successful charge updates payment and confirms booking")
    void processPayment_Success() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findByIdWithPassengers(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult result = MockPaymentGateway.GatewayResult.success("TXN-12345", "approved");
        when(paymentGateway.charge(anyString(), any(), anyString(), any())).thenReturn(result);

        PaymentResponse resp = paymentService.processPayment(req);

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("INR", resp.getCurrency());
        assertEquals(Booking.BookingStatus.CONFIRMED, testBooking.getStatus());
        verify(paymentGateway).charge(eq("CREDIT_CARD"), eq(testBooking.getTotalAmount()), eq("INR"), any());
        verify(seatService, times(1)).confirmSeats(List.of(50L));
    }

    @Test
    @DisplayName("processPayment - failed charge sets payment status to FAILED")
    void processPayment_Failure() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findByIdWithPassengers(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult result = MockPaymentGateway.GatewayResult.failure("DECLINED", "Card declined");
        when(paymentGateway.charge(anyString(), any(), anyString(), any())).thenReturn(result);

        PaymentResponse resp = paymentService.processPayment(req);

        assertNotNull(resp);
        assertEquals("FAILED", resp.getStatus());
        assertEquals(Booking.BookingStatus.PENDING, testBooking.getStatus()); // remains pending
        verify(seatService, never()).confirmSeats(any());
    }

    @Test
    @DisplayName("processPayment - throws forbidden when booking belongs to another user")
    void processPayment_WrongUser_ThrowsForbidden() {
        testUser.setEmail("other@example.com");

        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findByIdWithPassengers(100L)).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> paymentService.processPayment(req));
    }

    @Test
    @DisplayName("processPayment - throws bad request when booking is not PENDING")
    void processPayment_AlreadyConfirmed_ThrowsBadRequest() {
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);

        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findByIdWithPassengers(100L)).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> paymentService.processPayment(req));
    }

    @Test
    @DisplayName("getPaymentByBookingId - returns payment for booking owner")
    void getPaymentByBookingId_Success() {
        when(paymentRepository.findByBookingIdWithDetails(100L)).thenReturn(Optional.of(testPayment));

        PaymentResponse resp = paymentService.getPaymentByBookingId(100L);

        assertNotNull(resp);
        assertEquals("TXN-12345", resp.getTransactionId());
    }

    @Test
    @DisplayName("processPayment - duplicate payment throws conflict")
    void processPayment_Duplicate_ThrowsConflict() {
        PaymentRequest req = new PaymentRequest();
        req.setBookingId(100L);
        req.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findByIdWithPassengers(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.existsByBookingId(100L)).thenReturn(true);

        assertThrows(ApiException.class, () -> paymentService.processPayment(req));
    }

    @Test
    @DisplayName("getPaymentByBookingId - admin can view another user's payment")
    void getPaymentByBookingId_Admin_Success() {
        testUser.setEmail("other@example.com");
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        when(paymentRepository.findByBookingIdWithDetails(100L)).thenReturn(Optional.of(testPayment));

        PaymentResponse resp = paymentService.getPaymentByBookingId(100L);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("getPaymentByBookingId - non-owner non-admin throws forbidden")
    void getPaymentByBookingId_NonOwner_ThrowsForbidden() {
        testUser.setEmail("other@example.com");
        when(paymentRepository.findByBookingIdWithDetails(100L)).thenReturn(Optional.of(testPayment));

        assertThrows(ApiException.class, () -> paymentService.getPaymentByBookingId(100L));
    }
}
