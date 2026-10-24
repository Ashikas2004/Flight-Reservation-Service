package com.flightreservation.service;

import com.flightreservation.dto.request.FlightRequest;
import com.flightreservation.dto.request.FlightSearchRequest;
import com.flightreservation.dto.response.FlightResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.Airline;
import com.flightreservation.entity.Airport;
import com.flightreservation.entity.Booking;
import com.flightreservation.entity.Flight;
import com.flightreservation.entity.Payment;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AirlineRepository;
import com.flightreservation.repository.AirportRepository;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.PaymentRepository;
import com.flightreservation.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private AirlineRepository airlineRepository;
    @Mock
    private AirportRepository airportRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private RefundService refundService;

    @InjectMocks
    private FlightService flightService;

    private Flight testFlight;
    private Airline testAirline;
    private Airport originAirport;
    private Airport destAirport;

    @BeforeEach
    void setUp() {
        testAirline = Airline.builder().id(1L).code("AI").name("Air India").logoUrl("logo.png").build();
        originAirport = Airport.builder().id(1L).iataCode("DEL").name("Delhi Airport").city("Delhi").country("India").build();
        destAirport = Airport.builder().id(2L).iataCode("BOM").name("Mumbai Airport").city("Mumbai").country("India").build();

        testFlight = Flight.builder()
                .id(100L)
                .flightNumber("AI-101")
                .airline(testAirline)
                .originAirport(originAirport)
                .destinationAirport(destAirport)
                .departureTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 12, 0))
                .basePrice(new BigDecimal("150.00"))
                .totalSeats(180)
                .availableSeats(180)
                .status(Flight.FlightStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("getAllFlights - returns paginated flights")
    void getAllFlights_Success() {
        Page<Flight> page = new PageImpl<>(List.of(testFlight));
        when(flightRepository.findAllWithDetails(any(PageRequest.class))).thenReturn(page);

        PageResponse<FlightResponse> result = flightService.getAllFlights(0, 10);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("AI-101", result.getContent().get(0).getFlightNumber());
    }

    @Test
    @DisplayName("searchFlights - returns matched flights based on search criteria")
    void searchFlights_Success() {
        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 12, 1));
        req.setPassengers(2);
        req.setSortBy("departureTime");
        req.setSortDir("asc");

        Page<Flight> page = new PageImpl<>(List.of(testFlight));
        when(flightRepository.searchFlights(eq("DEL"), eq("BOM"), any(LocalDateTime.class), any(LocalDateTime.class), eq(2), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<FlightResponse> result = flightService.searchFlights(req);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("AI-101", result.getContent().get(0).getFlightNumber());
    }

    @Test
    @DisplayName("searchFlights - supports descending sort direction")
    void searchFlights_Desc_Success() {
        FlightSearchRequest req = new FlightSearchRequest();
        req.setOrigin("DEL");
        req.setDestination("BOM");
        req.setDepartureDate(LocalDate.of(2026, 12, 1));
        req.setPassengers(1);
        req.setSortBy("basePrice");
        req.setSortDir("desc");

        Page<Flight> page = new PageImpl<>(List.of(testFlight));
        when(flightRepository.searchFlights(eq("DEL"), eq("BOM"), any(LocalDateTime.class), any(LocalDateTime.class), eq(1), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<FlightResponse> result = flightService.searchFlights(req);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    @DisplayName("getFlightById - returns flight when ID exists")
    void getFlightById_Success() {
        when(flightRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(testFlight));

        FlightResponse result = flightService.getFlightById(100L);

        assertNotNull(result);
        assertEquals("AI-101", result.getFlightNumber());
    }

    @Test
    @DisplayName("getFlightById - throws not found when ID does not exist")
    void getFlightById_NotFound_ThrowsException() {
        when(flightRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> flightService.getFlightById(999L));
    }

    @Test
    @DisplayName("completeArrivedFlights - marks scheduled and delayed flights completed")
    void completeArrivedFlights_UpdatesEligibleFlights() {
        when(flightRepository.completeArrivedFlights(
                any(LocalDateTime.class),
                eq(List.of(Flight.FlightStatus.SCHEDULED, Flight.FlightStatus.DELAYED)),
                eq(Flight.FlightStatus.COMPLETED)))
                .thenReturn(2);

        flightService.completeArrivedFlights();

        verify(flightRepository).completeArrivedFlights(
                any(LocalDateTime.class),
                eq(List.of(Flight.FlightStatus.SCHEDULED, Flight.FlightStatus.DELAYED)),
                eq(Flight.FlightStatus.COMPLETED));
    }

    @Test
    @DisplayName("createFlight - creates flight and generates seats")
    void createFlight_Success() {
        FlightRequest req = new FlightRequest();
        req.setFlightNumber("AI-202");
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.now().plusDays(1));
        req.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        req.setBasePrice(new BigDecimal("200.00"));
        req.setFirstClassSeats(4);
        req.setBusinessSeats(6);
        req.setEconomySeats(12);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.save(any(Flight.class))).thenReturn(testFlight);

        FlightResponse result = flightService.createFlight(req);

        assertNotNull(result);
        verify(flightRepository, times(1)).save(any(Flight.class));
        verify(seatRepository).saveAll(argThat(seats -> {
            boolean hasFirstClassSeat = false;
            boolean hasBusinessSeat = false;
            for (Seat seat : seats) {
                if (seat.getSeatClass() == Seat.SeatClass.FIRST) {
                    hasFirstClassSeat = true;
                    if (seat.getExtraPrice().compareTo(new BigDecimal("6000")) != 0) return false;
                } else if (seat.getSeatClass() == Seat.SeatClass.BUSINESS) {
                    hasBusinessSeat = true;
                    if (seat.getExtraPrice().compareTo(new BigDecimal("3000")) != 0) return false;
                }
            }
            return hasFirstClassSeat && hasBusinessSeat;
        }));
    }

    @Test
    @DisplayName("createFlight - generates daily bookable occurrences through the schedule end date")
    void createFlight_DailyRoutine_GeneratesOccurrences() {
        FlightRequest req = buildValidFlightRequest("AI-203");
        req.setScheduleRoutine("DAILY");
        req.setRecurrenceEndDate(req.getDepartureTime().toLocalDate().plusDays(2));

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightResponse result = flightService.createFlight(req);

        assertNotNull(result);
        assertEquals("DAILY", result.getScheduleRoutine());
        assertEquals(req.getDepartureTime().toLocalDate().plusDays(2), result.getRecurrenceEndDate());
        verify(flightRepository, times(3)).save(any(Flight.class));
        verify(seatRepository, times(3)).saveAll(anyList());
    }

    @Test
    @DisplayName("createFlight - keeps the first flight and uses the selected weekday afterward")
    void createFlight_WeeklyRoutine_UsesSelectedWeekdayAfterFirstFlight() {
        List<Flight> flights = createFlightOccurrences(
                "WEEKLY", LocalDateTime.of(2026, 12, 1, 10, 0), LocalDate.of(2026, 12, 15), 4, null);

        assertEquals(List.of(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 10)),
                flights.stream().map(flight -> flight.getDepartureTime().toLocalDate())
                .toList());
        assertTrue(flights.subList(1, flights.size()).stream().allMatch(flight ->
                flight.getDepartureTime().getDayOfWeek().getValue() == 4));
    }

    @Test
    @DisplayName("createFlight - monthly routine uses the selected day and skips months without it")
    void createFlight_MonthlyRoutine_UsesSelectedDay() {
        List<Flight> flights = createFlightOccurrences(
                "MONTHLY", LocalDateTime.of(2026, 1, 31, 10, 0), LocalDate.of(2026, 3, 31), null, 31);

        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31)),
                flights.stream().map(flight -> flight.getDepartureTime().toLocalDate()).toList());
    }

    @Test
    @DisplayName("createFlight - monthly routine can use a selected date different from the first flight")
    void createFlight_MonthlyRoutine_UsesSelectedDateAfterFirstFlight() {
        List<Flight> flights = createFlightOccurrences(
                "MONTHLY", LocalDateTime.of(2026, 1, 31, 10, 0), LocalDate.of(2026, 3, 31), null, 15);

        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 15),
                        LocalDate.of(2026, 3, 15)),
                flights.stream().map(flight -> flight.getDepartureTime().toLocalDate()).toList());
    }

    @Test
    @DisplayName("createFlight - alternate odd routine uses odd calendar dates")
    void createFlight_AlternateOddRoutine_UsesOddCalendarDates() {
        List<Flight> flights = createFlightOccurrences(
                "ALTERNATE_ODD", LocalDateTime.of(2026, 1, 31, 10, 0), LocalDate.of(2026, 2, 5), null, null);

        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 2, 3), LocalDate.of(2026, 2, 5)),
                flights.stream().map(flight -> flight.getDepartureTime().toLocalDate()).toList());
    }

    @Test
    @DisplayName("createFlight - throws conflict when flight number already exists")
    void createFlight_DuplicateNumber_ThrowsConflict() {
        FlightRequest req = buildValidFlightRequest("AI-101");

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.existsByFlightNumberAndDepartureTime("AI-101", req.getDepartureTime()))
                .thenReturn(true);

        assertThrows(ApiException.class, () -> flightService.createFlight(req));
    }

    @Test
    @DisplayName("createFlight - throws bad request when arrival is before departure")
    void createFlight_InvalidTimes_ThrowsBadRequest() {
        FlightRequest req = new FlightRequest();
        req.setFlightNumber("AI-303");
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.now().plusDays(1).plusHours(4));
        req.setArrivalTime(LocalDateTime.now().plusDays(1)); // before departure!

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));

        assertThrows(ApiException.class, () -> flightService.createFlight(req));
    }

    private FlightRequest buildValidFlightRequest(String flightNumber) {
        FlightRequest req = new FlightRequest();
        req.setFlightNumber(flightNumber);
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.of(2026, 12, 1, 10, 0));
        req.setArrivalTime(LocalDateTime.of(2026, 12, 1, 12, 0));
        req.setBasePrice(new BigDecimal("200.00"));
        req.setFirstClassSeats(4);
        req.setBusinessSeats(6);
        req.setEconomySeats(12);
        return req;
    }

    private List<Flight> createFlightOccurrences(
            String routine,
            LocalDateTime departure,
            LocalDate endDate,
            Integer weeklyDay,
            Integer monthlyDay) {
        FlightRequest req = buildValidFlightRequest("AI-204");
        req.setDepartureTime(departure);
        req.setArrivalTime(departure.plusHours(2));
        req.setScheduleRoutine(routine);
        req.setRecurrenceEndDate(endDate);
        req.setWeeklyDay(weeklyDay);
        req.setMonthlyDay(monthlyDay);

        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));

        flightService.createFlight(req);

        org.mockito.ArgumentCaptor<Flight> flightsCaptor = org.mockito.ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository, atLeastOnce()).save(flightsCaptor.capture());
        return new ArrayList<>(flightsCaptor.getAllValues());
    }

    @Test
    @DisplayName("updateFlight - updates flight details")
    void updateFlight_Success() {
        FlightRequest req = new FlightRequest();
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.now().plusDays(2));
        req.setArrivalTime(LocalDateTime.now().plusDays(2).plusHours(3));
        req.setBasePrice(new BigDecimal("220.00"));

        when(flightRepository.findById(100L)).thenReturn(Optional.of(testFlight));
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.save(any(Flight.class))).thenReturn(testFlight);

        FlightResponse result = flightService.updateFlight(100L, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("220.00"), testFlight.getBasePrice());
    }

    @Test
    @DisplayName("updateFlight - creates missing dated flight records and seats for recurring schedules")
    void updateFlight_RecurringRoutine_CreatesOccurrencesAndSeats() {
        FlightRequest req = new FlightRequest();
        req.setAirlineId(1L);
        req.setOriginAirportId(1L);
        req.setDestinationAirportId(2L);
        req.setDepartureTime(LocalDateTime.of(2026, 12, 1, 10, 0));
        req.setArrivalTime(LocalDateTime.of(2026, 12, 1, 12, 0));
        req.setBasePrice(new BigDecimal("220.00"));
        req.setScheduleRoutine("DAILY");
        req.setRecurrenceEndDate(LocalDate.of(2026, 12, 3));

        List<Seat> sourceSeats = List.of(
                Seat.builder()
                        .flight(testFlight)
                        .seatNumber("1A")
                        .seatClass(Seat.SeatClass.FIRST)
                        .status(Seat.SeatStatus.BOOKED)
                        .extraPrice(new BigDecimal("6000"))
                        .build(),
                Seat.builder()
                        .flight(testFlight)
                        .seatNumber("3B")
                        .seatClass(Seat.SeatClass.ECONOMY)
                        .status(Seat.SeatStatus.AVAILABLE)
                        .extraPrice(BigDecimal.ZERO)
                        .build());

        when(flightRepository.findById(100L)).thenReturn(Optional.of(testFlight));
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(testAirline));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(originAirport));
        when(airportRepository.findById(2L)).thenReturn(Optional.of(destAirport));
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatRepository.findByFlightId(100L)).thenReturn(sourceSeats);

        FlightResponse response = flightService.updateFlight(100L, req);

        assertEquals("DAILY", response.getScheduleRoutine());
        assertEquals(LocalDate.of(2026, 12, 3), response.getRecurrenceEndDate());
        org.mockito.ArgumentCaptor<Flight> flightsCaptor =
                org.mockito.ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository, times(3)).save(flightsCaptor.capture());
        List<Flight> savedFlights = flightsCaptor.getAllValues();
        assertEquals(List.of(
                        LocalDateTime.of(2026, 12, 1, 10, 0),
                        LocalDateTime.of(2026, 12, 2, 10, 0),
                        LocalDateTime.of(2026, 12, 3, 10, 0)),
                savedFlights.stream().map(Flight::getDepartureTime).toList());
        assertEquals(savedFlights.get(0).getScheduleSeriesId(), savedFlights.get(1).getScheduleSeriesId());
        verify(seatRepository, times(2)).saveAll(argThat(seats -> {
            int count = 0;
            for (Seat seat : seats) {
                if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) return false;
                count++;
            }
            return count == 2;
        }));
    }

    @Test
    @DisplayName("updateFlightStatus - updates status enum")
    void updateFlightStatus_Success() {
        when(flightRepository.findById(100L)).thenReturn(Optional.of(testFlight));

        flightService.updateFlightStatus(100L, "DELAYED");

        assertEquals(Flight.FlightStatus.DELAYED, testFlight.getStatus());
        verify(flightRepository, times(1)).save(testFlight);
        verifyNoInteractions(bookingRepository, paymentRepository, refundService);
    }

    @Test
    @DisplayName("updateFlightStatus - cancels confirmed bookings and refunds successful payments")
    void updateFlightStatus_CancelledFlight_RefundsConfirmedBookings() {
        Booking booking = Booking.builder()
                .id(200L)
                .bookingReference("BOOK1234")
                .flight(testFlight)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        Payment payment = Payment.builder()
                .id(300L)
                .booking(booking)
                .amount(new BigDecimal("250.00"))
                .status(Payment.PaymentStatus.SUCCESS)
                .build();

        when(flightRepository.findById(100L)).thenReturn(Optional.of(testFlight));
        when(bookingRepository.findConfirmedBookingsByFlightId(100L)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingId(200L)).thenReturn(Optional.of(payment));

        flightService.updateFlightStatus(100L, "CANCELLED");

        assertEquals(Flight.FlightStatus.CANCELLED, testFlight.getStatus());
        assertEquals(Booking.BookingStatus.CANCELLED, booking.getStatus());
        assertEquals("Flight cancelled by airline: AI-101", booking.getCancellationReason());
        assertNotNull(booking.getCancelledAt());
        verify(refundService).processAutomaticRefund(
                booking, payment, "Flight cancelled by airline: AI-101", "system");
    }

    @Test
    @DisplayName("generateSeats - legacy seed method generates default seat layout")
    void generateSeats_Success() {
        flightService.generateSeats(testFlight);

        verify(seatRepository, times(1)).saveAll(anyList());
    }
}
