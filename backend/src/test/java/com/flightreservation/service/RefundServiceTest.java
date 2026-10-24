package com.flightreservation.service;

import com.flightreservation.dto.request.RefundRequest;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.dto.response.RefundResponse;
import com.flightreservation.entity.*;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.PaymentRepository;
import com.flightreservation.repository.RefundRepository;
import com.flightreservation.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private MockPaymentGateway paymentGateway;

    @InjectMocks
    private RefundService refundService;

    private User testUser;
    private Booking testBooking;
    private Payment testPayment;
    private Refund testRefund;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "passenger@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        testUser = User.builder().id(1L).email("passenger@example.com").build();

        testBooking = Booking.builder()
                .id(100L)
                .bookingReference("BK-REFUND")
                .user(testUser)
                .status(Booking.BookingStatus.CANCELLED)
                .totalAmount(new BigDecimal("300.00"))
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .booking(testBooking)
                .amount(new BigDecimal("300.00"))
                .currency("INR")
                .transactionId("TXN-PAY")
                .status(Payment.PaymentStatus.SUCCESS)
                .build();

        testRefund = Refund.builder()
                .id(1L)
                .booking(testBooking)
                .payment(testPayment)
                .refundAmount(new BigDecimal("300.00"))
                .currency("INR")
                .status(Refund.RefundStatus.PROCESSED)
                .refundTransactionId("RFD-12345")
                .processedAt(LocalDateTime.now())
                .build();
        testRefund.setCreatedAt(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("requestRefund - processes refund through gateway when valid")
    void requestRefund_Success() {
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);
        req.setReason("Trip cancelled");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(testPayment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult result = MockPaymentGateway.GatewayResult.success("RFD-12345", "approved");
        when(paymentGateway.refund(eq("TXN-PAY"), any(BigDecimal.class), eq("INR"))).thenReturn(result);

        RefundResponse resp = refundService.requestRefund(req);

        assertNotNull(resp);
        assertEquals("PROCESSED", resp.getStatus());
        assertEquals(Booking.BookingStatus.REFUNDED, testBooking.getStatus());
        assertEquals(Payment.PaymentStatus.REFUNDED, testPayment.getStatus());
    }

    @Test
    @DisplayName("requestRefund - throws bad request when booking is not cancelled")
    void requestRefund_NotCancelled_ThrowsBadRequest() {
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);

        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> refundService.requestRefund(req));
    }

    @Test
    @DisplayName("processAutomaticRefund - automatically processes refund for cancelled booking")
    void processAutomaticRefund_Success() {
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult result = MockPaymentGateway.GatewayResult.success("RFD-AUTO", "approved");
        when(paymentGateway.refund(eq("TXN-PAY"), any(BigDecimal.class), eq("INR"))).thenReturn(result);

        Refund refund = refundService.processAutomaticRefund(testBooking, testPayment, "Auto refund", "admin@example.com");

        assertNotNull(refund);
        assertEquals(Refund.RefundStatus.PROCESSED, refund.getStatus());
        assertEquals("RFD-AUTO", refund.getRefundTransactionId());
    }

    @Test
    @DisplayName("getMyRefunds - returns paginated user refunds")
    void getMyRefunds_Success() {
        Page<Refund> page = new PageImpl<>(List.of(testRefund));
        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(testUser));
        when(refundRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<RefundResponse> resp = refundService.getMyRefunds(0, 10);

        assertNotNull(resp);
        assertEquals(1, resp.getContent().size());
        assertEquals("RFD-12345", resp.getContent().get(0).getRefundTransactionId());
    }

    @Test
    @DisplayName("getByBookingId - returns refund details for booking")
    void getByBookingId_Success() {
        when(refundRepository.findByBookingId(100L)).thenReturn(Optional.of(testRefund));

        RefundResponse resp = refundService.getByBookingId(100L);

        assertNotNull(resp);
        assertEquals("RFD-12345", resp.getRefundTransactionId());
    }

    @Test
    @DisplayName("requestRefund - admin can request refund for another user")
    void requestRefund_AdminAccess_Success() {
        testUser.setEmail("other@example.com");
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(testPayment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult result = MockPaymentGateway.GatewayResult.success("RFD-123", "ok");
        when(paymentGateway.refund(any(), any(), any())).thenReturn(result);

        RefundResponse resp = refundService.requestRefund(req);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("requestRefund - non-owner non-admin throws forbidden")
    void requestRefund_Forbidden_ThrowsException() {
        testUser.setEmail("other@example.com");
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        assertThrows(ApiException.class, () -> refundService.requestRefund(req));
    }

    @Test
    @DisplayName("requestRefund - duplicate refund throws conflict")
    void requestRefund_Duplicate_ThrowsConflict() {
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(refundRepository.existsByBookingId(100L)).thenReturn(true);

        assertThrows(ApiException.class, () -> refundService.requestRefund(req));
    }

    @Test
    @DisplayName("requestRefund - non-successful payment throws bad request")
    void requestRefund_PaymentFailed_ThrowsBadRequest() {
        testPayment.setStatus(Payment.PaymentStatus.FAILED);
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(testPayment));

        assertThrows(ApiException.class, () -> refundService.requestRefund(req));
    }

    @Test
    @DisplayName("requestRefund - gateway failure marks refund as FAILED")
    void requestRefund_GatewayFailure() {
        RefundRequest req = new RefundRequest();
        req.setBookingId(100L);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(testPayment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult failedResult = MockPaymentGateway.GatewayResult.failure("DECLINED", "Refund declined");
        when(paymentGateway.refund(any(), any(), any())).thenReturn(failedResult);

        RefundResponse resp = refundService.requestRefund(req);
        assertNotNull(resp);
        assertEquals("FAILED", resp.getStatus());
    }

    @Test
    @DisplayName("processAutomaticRefund - duplicate refund returns existing")
    void processAutomaticRefund_Duplicate_ReturnsExisting() {
        when(refundRepository.existsByBookingId(100L)).thenReturn(true);
        when(refundRepository.findByBookingId(100L)).thenReturn(Optional.of(testRefund));

        Refund result = refundService.processAutomaticRefund(testBooking, testPayment, null, "admin@example.com");
        assertEquals(testRefund, result);
    }

    @Test
    @DisplayName("processAutomaticRefund - gateway failure and null reason default")
    void processAutomaticRefund_GatewayFailure_NullReason() {
        when(refundRepository.existsByBookingId(100L)).thenReturn(false);
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));

        MockPaymentGateway.GatewayResult failedResult = MockPaymentGateway.GatewayResult.failure("FAIL", "Error");
        when(paymentGateway.refund(any(), any(), any())).thenReturn(failedResult);

        Refund refund = refundService.processAutomaticRefund(testBooking, testPayment, "  ", "system@example.com");
        assertNotNull(refund);
        assertEquals(Refund.RefundStatus.FAILED, refund.getStatus());
        assertEquals("Automatic refund upon booking cancellation", refund.getReason());
    }
}
