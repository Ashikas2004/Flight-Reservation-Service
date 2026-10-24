package com.flightreservation.dto;

import com.flightreservation.dto.request.*;
import com.flightreservation.dto.response.*;
import com.flightreservation.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoAndEntityTest {

    @Test
    @DisplayName("PageResponse - test Page factory method and getters")
    void testPageResponse() {
        Page<String> page = new PageImpl<>(List.of("item1", "item2"));
        PageResponse<String> resp = PageResponse.of(page);

        assertNotNull(resp);
        assertEquals(2, resp.getContent().size());
        assertEquals(0, resp.getPageNumber());
        assertEquals(2, resp.getTotalElements());
        assertEquals(1, resp.getTotalPages());
        assertTrue(resp.isLast());

        resp.setPageNumber(1);
        resp.setPageSize(20);
        resp.setTotalElements(50);
        resp.setTotalPages(3);
        resp.setLast(false);
        resp.setContent(List.of("item3"));

        assertEquals(1, resp.getPageNumber());
        assertEquals(20, resp.getPageSize());
        assertEquals(50, resp.getTotalElements());
        assertEquals(3, resp.getTotalPages());
        assertFalse(resp.isLast());
        assertNotNull(resp.toString());
    }

    @Test
    @DisplayName("Request DTOs - test getters, setters, constructors")
    void testRequestDtos() {
        // RegisterRequest
        RegisterRequest reg = new RegisterRequest();
        reg.setFirstName("First");
        reg.setLastName("Last");
        reg.setEmail("test@email.com");
        reg.setPassword("password123");
        reg.setPhoneNumber("1234567890");
        assertEquals("First", reg.getFirstName());
        assertEquals("Last", reg.getLastName());
        assertEquals("test@email.com", reg.getEmail());
        assertEquals("password123", reg.getPassword());
        assertEquals("1234567890", reg.getPhoneNumber());
        assertNotNull(reg.toString());

        // LoginRequest
        LoginRequest login = new LoginRequest();
        login.setEmail("test@email.com");
        login.setPassword("password123");
        assertEquals("test@email.com", login.getEmail());
        assertEquals("password123", login.getPassword());
        assertNotNull(login.toString());

        // FlightSearchRequest
        FlightSearchRequest search = new FlightSearchRequest();
        search.setOrigin("DEL");
        search.setDestination("BOM");
        search.setDepartureDate(LocalDate.now());
        search.setPassengers(2);
        search.setPage(1);
        search.setSize(20);
        search.setSortBy("price");
        search.setSortDir("desc");
        assertEquals("DEL", search.getOrigin());
        assertEquals("BOM", search.getDestination());
        assertEquals(2, search.getPassengers());
        assertEquals(1, search.getPage());
        assertEquals(20, search.getSize());
        assertEquals("price", search.getSortBy());
        assertEquals("desc", search.getSortDir());

        // FlightRequest
        FlightRequest flightReq = new FlightRequest();
        flightReq.setFlightNumber("AI-101");
        flightReq.setAirlineId(1L);
        flightReq.setOriginAirportId(2L);
        flightReq.setDestinationAirportId(3L);
        flightReq.setDepartureTime(LocalDateTime.now());
        flightReq.setArrivalTime(LocalDateTime.now().plusHours(2));
        flightReq.setBasePrice(new BigDecimal("150.00"));
        flightReq.setFirstClassSeats(4);
        flightReq.setBusinessSeats(6);
        flightReq.setEconomySeats(100);
        assertEquals("AI-101", flightReq.getFlightNumber());
        assertEquals(1L, flightReq.getAirlineId());
        assertEquals(new BigDecimal("150.00"), flightReq.getBasePrice());

        // PassengerRequest
        PassengerRequest pr = new PassengerRequest();
        pr.setSeatId(10L);
        pr.setFirstName("John");
        pr.setLastName("Doe");
        pr.setPassportNumber("P123");
        pr.setDateOfBirth("1995-05-05");
        pr.setNationality("Indian");
        assertEquals(10L, pr.getSeatId());
        assertEquals("John", pr.getFirstName());

        // PaymentRequest
        PaymentRequest payReq = new PaymentRequest();
        payReq.setBookingId(1L);
        payReq.setPaymentMethod("CARD");
        payReq.setCardNumber("1111222233334444");
        payReq.setCardHolderName("John");
        payReq.setExpiryMonth("12");
        payReq.setExpiryYear("2030");
        payReq.setCvv("123");
        assertEquals(1L, payReq.getBookingId());
        assertEquals("CARD", payReq.getPaymentMethod());

        // RefundRequest
        RefundRequest refReq = new RefundRequest();
        refReq.setBookingId(2L);
        refReq.setReason("Cancel");
        assertEquals(2L, refReq.getBookingId());
        assertEquals("Cancel", refReq.getReason());

        // SeatLockRequest & SeatReleaseRequest
        SeatLockRequest lockReq = new SeatLockRequest();
        lockReq.setFlightId(5L);
        lockReq.setSeatIds(List.of(1L, 2L));
        assertEquals(5L, lockReq.getFlightId());
        assertEquals(2, lockReq.getSeatIds().size());

        SeatReleaseRequest relReq = new SeatReleaseRequest();
        relReq.setSeatIds(List.of(1L));
        assertEquals(1, relReq.getSeatIds().size());

        // SeatRequest
        SeatRequest seatReq = new SeatRequest();
        seatReq.setFlightId(1L);
        seatReq.setSeatNumber("1A");
        seatReq.setSeatClass("FIRST");
        seatReq.setExtraPrice(new BigDecimal("50.00"));
        assertEquals("1A", seatReq.getSeatNumber());

        // AirlineRequest & AirlineUpdateRequest
        AirlineRequest aReq = new AirlineRequest();
        aReq.setCode("AI");
        aReq.setName("Air India");
        aReq.setCountry("India");
        aReq.setLogoUrl("logo.png");
        assertEquals("AI", aReq.getCode());

        AirlineUpdateRequest aUpReq = new AirlineUpdateRequest();
        aUpReq.setName("New Name");
        aUpReq.setCountry("India");
        aUpReq.setLogoUrl("new.png");
        assertEquals("New Name", aUpReq.getName());

        // AirportRequest
        AirportRequest airReq = new AirportRequest();
        airReq.setIataCode("DEL");
        airReq.setName("Delhi");
        airReq.setCity("Delhi");
        airReq.setCountry("India");
        airReq.setTimezone("IST");
        assertEquals("DEL", airReq.getIataCode());
    }

    @Test
    @DisplayName("Response DTOs - test getters, setters, builders, equality")
    void testResponseDtos() {
        AirlineResponse airline = AirlineResponse.builder()
                .id(1L).code("AI").name("Air India").country("India").logoUrl("logo.png").active(true).build();
        assertEquals(1L, airline.getId());
        assertEquals("AI", airline.getCode());
        assertNotNull(airline.toString());

        AirportResponse airport = AirportResponse.builder()
                .id(1L).iataCode("DEL").name("Delhi").city("Delhi").country("India").timezone("IST").active(true).build();
        assertEquals("DEL", airport.getIataCode());

        SeatResponse seat = SeatResponse.builder()
                .id(1L).seatNumber("1A").seatClass("FIRST").status("AVAILABLE").extraPrice(BigDecimal.ZERO).flightId(10L).isLockedByCurrentUser(false).build();
        assertEquals("1A", seat.getSeatNumber());

        PaymentResponse payment = PaymentResponse.builder()
                .id(1L).bookingId(10L).bookingReference("BK123").transactionId("TXN1").amount(new BigDecimal("100")).currency("USD").paymentMethod("CARD").status("SUCCESS").paidAt(LocalDateTime.now()).failedReason(null).build();
        assertEquals("TXN1", payment.getTransactionId());

        RefundResponse refund = RefundResponse.builder()
                .id(1L).bookingId(10L).bookingReference("BK123").refundAmount(new BigDecimal("100")).currency("USD").status("PROCESSED").reason("Test").refundTransactionId("RFD1").processedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
        assertEquals("RFD1", refund.getRefundTransactionId());

        AuditLogResponse audit = AuditLogResponse.builder()
                .id(1L).action("LOGIN").entityType("User").entityId(1L).performedBy("admin").description("desc").ipAddress("127.0.0.1").createdAt(LocalDateTime.now()).build();
        assertEquals("LOGIN", audit.getAction());

        AuthResponse auth = AuthResponse.builder()
                .accessToken("token").refreshToken("refresh").tokenType("Bearer").expiresIn(3600L)
                .user(AuthResponse.UserInfo.builder().id(1L).email("u@test.com").firstName("F").lastName("L").role("ROLE_USER").build())
                .build();
        assertEquals("token", auth.getAccessToken());
        assertEquals("u@test.com", auth.getUser().getEmail());

        FlightResponse flight = FlightResponse.builder()
                .id(1L).flightNumber("AI-101")
                .airline(FlightResponse.AirlineInfo.builder().id(1L).code("AI").name("Air India").logoUrl("logo.png").build())
                .originAirport(FlightResponse.AirportInfo.builder().id(1L).iataCode("DEL").name("Delhi").city("Delhi").country("India").build())
                .destinationAirport(FlightResponse.AirportInfo.builder().id(2L).iataCode("BOM").name("Mumbai").city("Mumbai").country("India").build())
                .departureTime(LocalDateTime.now()).arrivalTime(LocalDateTime.now().plusHours(2))
                .basePrice(new BigDecimal("100.00")).totalSeats(180).availableSeats(100).status("SCHEDULED").durationMinutes(120)
                .build();
        assertEquals("AI-101", flight.getFlightNumber());
        assertEquals("DEL", flight.getOriginAirport().getIataCode());
    }

    @Test
    @DisplayName("Entities - test entity fields, enums, relationships")
    void testEntities() {
        User user = User.builder()
                .id(1L).email("user@test.com").password("pass").firstName("First").lastName("Last").phoneNumber("123").role(User.Role.PASSENGER).enabled(true).build();
        assertEquals("user@test.com", user.getEmail());
        assertEquals(User.Role.PASSENGER, user.getRole());

        Airline airline = Airline.builder().id(1L).code("AI").name("Air India").active(true).build();
        Airport origin = Airport.builder().id(1L).iataCode("DEL").build();
        Airport dest = Airport.builder().id(2L).iataCode("BOM").build();

        Flight flight = Flight.builder()
                .id(1L).flightNumber("AI-101").airline(airline).originAirport(origin).destinationAirport(dest).status(Flight.FlightStatus.SCHEDULED).build();
        assertEquals(Flight.FlightStatus.SCHEDULED, flight.getStatus());

        Seat seat = Seat.builder()
                .id(1L).flight(flight).seatNumber("1A").seatClass(Seat.SeatClass.FIRST).status(Seat.SeatStatus.AVAILABLE).extraPrice(BigDecimal.ZERO).lockedBy("user@test.com").lockedAt(LocalDateTime.now()).build();
        assertEquals(Seat.SeatClass.FIRST, seat.getSeatClass());
        assertEquals(Seat.SeatStatus.AVAILABLE, seat.getStatus());

        Booking booking = Booking.builder()
                .id(1L).bookingReference("BK123").user(user).flight(flight).status(Booking.BookingStatus.CONFIRMED).totalAmount(new BigDecimal("100.00")).bookedAt(LocalDateTime.now()).passengers(List.of()).build();
        assertEquals("BK123", booking.getBookingReference());
        assertEquals(Booking.BookingStatus.CONFIRMED, booking.getStatus());

        BookingPassenger bp = BookingPassenger.builder()
                .id(1L).booking(booking).seat(seat).firstName("John").lastName("Doe").passportNumber("P123").dateOfBirth("1990-01-01").nationality("Indian").build();
        assertEquals("John", bp.getFirstName());

        Payment payment = Payment.builder()
                .id(1L).booking(booking).amount(new BigDecimal("100.00")).currency("USD").paymentMethod(Payment.PaymentMethod.CREDIT_CARD).status(Payment.PaymentStatus.SUCCESS).transactionId("TXN1").paidAt(LocalDateTime.now()).build();
        assertEquals(Payment.PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(Payment.PaymentMethod.CREDIT_CARD, payment.getPaymentMethod());

        Refund refund = Refund.builder()
                .id(1L).booking(booking).payment(payment).refundAmount(new BigDecimal("100.00")).currency("USD").status(Refund.RefundStatus.PROCESSED).reason("Cancel").refundTransactionId("RFD1").build();
        assertEquals(Refund.RefundStatus.PROCESSED, refund.getStatus());

        AuditLog log = AuditLog.builder()
                .id(1L).action(AuditLog.AuditAction.BOOKING_CONFIRMED).entityType("Booking").entityId(1L).performedBy("admin").description("Confirmed").build();
        assertEquals(AuditLog.AuditAction.BOOKING_CONFIRMED, log.getAction());
    }
}

