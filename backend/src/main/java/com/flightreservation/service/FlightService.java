package com.flightreservation.service;

import com.flightreservation.dto.request.FlightRequest;
import com.flightreservation.dto.request.FlightSearchRequest;
import com.flightreservation.dto.response.FlightResponse;
import com.flightreservation.dto.response.PageResponse;
import com.flightreservation.entity.Airline;
import com.flightreservation.entity.Airport;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.Booking;
import com.flightreservation.entity.Flight;
import com.flightreservation.entity.Payment;
import com.flightreservation.entity.Seat;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.AirlineRepository;
import com.flightreservation.repository.AirportRepository;
import com.flightreservation.repository.BookingRepository;
import com.flightreservation.repository.FlightRepository;
import com.flightreservation.repository.PaymentRepository;
import com.flightreservation.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final AirportRepository airportRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefundService refundService;

    @Transactional(readOnly = true)
    public PageResponse<FlightResponse> getAllFlights(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return PageResponse.of(flightRepository.findAllWithDetails(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<FlightResponse> searchFlights(FlightSearchRequest req) {
        LocalDateTime from = req.getDepartureDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);

        Sort sort = req.getSortDir().equalsIgnoreCase("desc")
                ? Sort.by(req.getSortBy()).descending()
                : Sort.by(req.getSortBy()).ascending();
        PageRequest pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<Flight> page = flightRepository.searchFlights(
                req.getOrigin().toUpperCase(),
                req.getDestination().toUpperCase(),
                from, to,
                req.getPassengers(),
                pageable);

        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ApiException.notFound("Flight not found: " + id));
        return toResponse(flight);
    }

    @Transactional
    public FlightResponse createFlight(FlightRequest req) {
        Airline airline = airlineRepository.findById(req.getAirlineId())
                .orElseThrow(() -> ApiException.notFound("Airline not found: " + req.getAirlineId()));
        Airport origin = airportRepository.findById(req.getOriginAirportId())
                .orElseThrow(() -> ApiException.notFound("Origin airport not found"));
        Airport destination = airportRepository.findById(req.getDestinationAirportId())
                .orElseThrow(() -> ApiException.notFound("Destination airport not found"));

        if (!req.getArrivalTime().isAfter(req.getDepartureTime())) {
            throw ApiException.badRequest("Arrival time must be after departure time");
        }

        Flight.ScheduleRoutine routine;
        String requestedRoutine = req.getScheduleRoutine() == null
                ? Flight.ScheduleRoutine.SPECIAL_ADDITIONAL.name()
                : req.getScheduleRoutine().toUpperCase();
        try {
            routine = Flight.ScheduleRoutine.valueOf(requestedRoutine);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unsupported flight schedule routine: " + req.getScheduleRoutine());
        }

        LocalDate firstDate = req.getDepartureTime().toLocalDate();
        LocalDate recurrenceEndDate = routine == Flight.ScheduleRoutine.SPECIAL_ADDITIONAL
                ? firstDate
                : req.getRecurrenceEndDate() != null ? req.getRecurrenceEndDate() : firstDate.plusDays(59);
        if (recurrenceEndDate.isBefore(firstDate)) {
            throw ApiException.badRequest("Schedule end date must be on or after the first departure date");
        }
        if (ChronoUnit.DAYS.between(firstDate, recurrenceEndDate) > 364) {
            throw ApiException.badRequest("A recurring schedule cannot exceed 365 days");
        }
        if (routine == Flight.ScheduleRoutine.ALTERNATE_ODD && firstDate.getDayOfMonth() % 2 == 0 ||
                routine == Flight.ScheduleRoutine.ALTERNATE_EVEN && firstDate.getDayOfMonth() % 2 != 0) {
            throw ApiException.badRequest("The first departure date does not match the selected odd/even routine");
        }
        validateRoutineDay(routine, req.getWeeklyDay(), req.getMonthlyDay());

        // First class uses 4 cols (A-D), Business and Economy use 6 cols (A-F)
        int firstRows    = (int) Math.ceil(req.getFirstClassSeats()  / 4.0);
        int businessRows = (int) Math.ceil(req.getBusinessSeats()    / 6.0);
        int economyRows  = (int) Math.ceil(req.getEconomySeats()     / 6.0);
        int totalSeats   = (firstRows * 4) + (businessRows * 6) + (economyRows * 6);

        String flightNumber = req.getFlightNumber().toUpperCase();
        List<LocalDate> departureDates = getDepartureDates(
                firstDate, recurrenceEndDate, routine, req.getWeeklyDay(), req.getMonthlyDay());
        Duration flightDuration = Duration.between(req.getDepartureTime(), req.getArrivalTime());
        String seriesId = routine == Flight.ScheduleRoutine.SPECIAL_ADDITIONAL
                ? null
                : UUID.randomUUID().toString();
        Flight firstFlight = null;

        for (LocalDate departureDate : departureDates) {
            LocalDateTime departureTime = LocalDateTime.of(departureDate, req.getDepartureTime().toLocalTime());
            if (flightRepository.existsByFlightNumberAndDepartureTime(flightNumber, departureTime)) {
                throw ApiException.conflict("Flight " + flightNumber + " already exists at " + departureTime);
            }
            Flight flight = Flight.builder()
                    .flightNumber(flightNumber)
                    .airline(airline)
                    .originAirport(origin)
                    .destinationAirport(destination)
                    .departureTime(departureTime)
                    .arrivalTime(departureTime.plus(flightDuration))
                    .scheduleRoutine(routine)
                    .scheduleSeriesId(seriesId)
                    .recurrenceEndDate(recurrenceEndDate)
                    .weeklyDay(routine == Flight.ScheduleRoutine.WEEKLY ? req.getWeeklyDay() : null)
                    .monthlyDay(routine == Flight.ScheduleRoutine.MONTHLY ? req.getMonthlyDay() : null)
                    .basePrice(req.getBasePrice())
                    .totalSeats(totalSeats)
                    .availableSeats(totalSeats)
                    .status(Flight.FlightStatus.SCHEDULED)
                    .build();

            flight = flightRepository.save(flight);
            generateSeatsFromRequest(flight, firstRows, businessRows, economyRows);
            if (firstFlight == null) firstFlight = flight;
        }

        log.info("Created {} flight occurrence(s) for {} using {} routine",
                departureDates.size(), flightNumber, routine);
        return toResponse(firstFlight);
    }

    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest req) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Flight not found: " + id));

        if (!req.getArrivalTime().isAfter(req.getDepartureTime())) {
            throw ApiException.badRequest("Arrival time must be after departure time");
        }

        Airline airline = airlineRepository.findById(req.getAirlineId())
                .orElseThrow(() -> ApiException.notFound("Airline not found"));
        Airport origin = airportRepository.findById(req.getOriginAirportId())
                .orElseThrow(() -> ApiException.notFound("Origin airport not found"));
        Airport destination = airportRepository.findById(req.getDestinationAirportId())
                .orElseThrow(() -> ApiException.notFound("Destination airport not found"));

        LocalDate firstDate = req.getDepartureTime().toLocalDate();
        Flight.ScheduleRoutine routine = parseScheduleRoutine(req.getScheduleRoutine());
        LocalDate recurrenceEndDate = routine == Flight.ScheduleRoutine.SPECIAL_ADDITIONAL
                ? firstDate
                : req.getRecurrenceEndDate() != null ? req.getRecurrenceEndDate() : firstDate.plusDays(59);
        validateRecurrence(routine, firstDate, recurrenceEndDate);
        validateRoutineDay(routine, req.getWeeklyDay(), req.getMonthlyDay());

        if (!flight.getDepartureTime().equals(req.getDepartureTime()) &&
                flightRepository.existsByFlightNumberAndDepartureTime(
                        flight.getFlightNumber(), req.getDepartureTime())) {
            throw ApiException.conflict(
                    "Flight " + flight.getFlightNumber() + " already exists at " + req.getDepartureTime());
        }

        flight.setAirline(airline);
        flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination);
        flight.setDepartureTime(req.getDepartureTime());
        flight.setArrivalTime(req.getArrivalTime());
        flight.setBasePrice(req.getBasePrice());
        flight.setScheduleRoutine(routine);
        flight.setRecurrenceEndDate(recurrenceEndDate);
        flight.setWeeklyDay(routine == Flight.ScheduleRoutine.WEEKLY ? req.getWeeklyDay() : null);
        flight.setMonthlyDay(routine == Flight.ScheduleRoutine.MONTHLY ? req.getMonthlyDay() : null);
        flight.setScheduleSeriesId(routine == Flight.ScheduleRoutine.SPECIAL_ADDITIONAL
                ? null
                : flight.getScheduleSeriesId() != null ? flight.getScheduleSeriesId() : UUID.randomUUID().toString());

        Flight savedFlight = flightRepository.save(flight);
        if (routine != Flight.ScheduleRoutine.SPECIAL_ADDITIONAL) {
            List<LocalDate> departureDates = getDepartureDates(
                    firstDate, recurrenceEndDate, routine, req.getWeeklyDay(), req.getMonthlyDay());
            createMissingOccurrences(savedFlight, departureDates,
                    Duration.between(req.getDepartureTime(), req.getArrivalTime()));
        }
        return toResponse(savedFlight);
    }

    private void createMissingOccurrences(
            Flight source,
            List<LocalDate> departureDates,
            Duration flightDuration) {
        List<Seat> sourceSeats = seatRepository.findByFlightId(source.getId());
        if (sourceSeats.isEmpty()) {
            log.error("Cannot create recurring flights for {}: source flight {} has no seats",
                    source.getFlightNumber(), source.getId());
            throw new IllegalStateException("Cannot create recurring flights without a source flight seat layout");
        }

        for (LocalDate departureDate : departureDates) {
            LocalDateTime departureTime = LocalDateTime.of(departureDate, source.getDepartureTime().toLocalTime());
            if (departureTime.equals(source.getDepartureTime()) ||
                    flightRepository.existsByFlightNumberAndDepartureTime(source.getFlightNumber(), departureTime)) {
                continue;
            }

            Flight occurrence = Flight.builder()
                    .flightNumber(source.getFlightNumber())
                    .airline(source.getAirline())
                    .originAirport(source.getOriginAirport())
                    .destinationAirport(source.getDestinationAirport())
                    .departureTime(departureTime)
                    .arrivalTime(departureTime.plus(flightDuration))
                    .scheduleRoutine(source.getScheduleRoutine())
                    .scheduleSeriesId(source.getScheduleSeriesId())
                    .recurrenceEndDate(source.getRecurrenceEndDate())
                    .weeklyDay(source.getWeeklyDay())
                    .monthlyDay(source.getMonthlyDay())
                    .basePrice(source.getBasePrice())
                    .totalSeats(source.getTotalSeats())
                    .availableSeats(source.getTotalSeats())
                    .status(Flight.FlightStatus.SCHEDULED)
                    .build();
            Flight savedOccurrence = flightRepository.save(occurrence);
            List<Seat> occurrenceSeats = sourceSeats.stream()
                    .map(seat -> Seat.builder()
                            .flight(savedOccurrence)
                            .seatNumber(seat.getSeatNumber())
                            .seatClass(seat.getSeatClass())
                            .status(Seat.SeatStatus.AVAILABLE)
                            .extraPrice(seat.getExtraPrice())
                            .build())
                    .toList();
            seatRepository.saveAll(occurrenceSeats);
        }
    }

    private List<LocalDate> getDepartureDates(
            LocalDate firstDate,
            LocalDate recurrenceEndDate,
            Flight.ScheduleRoutine routine,
            Integer weeklyDay,
            Integer monthlyDay) {
        List<LocalDate> dates = new ArrayList<>();
        dates.add(firstDate);
        if (routine == Flight.ScheduleRoutine.SPECIAL_ADDITIONAL) return dates;

        LocalDate nextDate = switch (routine) {
            case DAILY -> firstDate.plusDays(1);
            case WEEKLY -> nextWeeklyDate(firstDate, weeklyDay);
            case ALTERNATE_ODD, ALTERNATE_EVEN -> nextAlternateDate(firstDate, routine);
            case MONTHLY -> nextMonthlyDate(firstDate, monthlyDay);
            case SPECIAL_ADDITIONAL -> firstDate;
        };

        while (!nextDate.isAfter(recurrenceEndDate)) {
            dates.add(nextDate);
            nextDate = switch (routine) {
                case DAILY -> nextDate.plusDays(1);
                case WEEKLY -> nextDate.plusWeeks(1);
                case ALTERNATE_ODD, ALTERNATE_EVEN -> nextAlternateDate(nextDate, routine);
                case MONTHLY -> nextMonthlyDate(nextDate, monthlyDay);
                case SPECIAL_ADDITIONAL -> recurrenceEndDate.plusDays(1);
            };
        }
        return dates;
    }

    private Flight.ScheduleRoutine parseScheduleRoutine(String requestedRoutine) {
        String routineName = requestedRoutine == null
                ? Flight.ScheduleRoutine.SPECIAL_ADDITIONAL.name()
                : requestedRoutine.toUpperCase();
        try {
            return Flight.ScheduleRoutine.valueOf(routineName);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unsupported flight schedule routine: " + requestedRoutine);
        }
    }

    private void validateRecurrence(
            Flight.ScheduleRoutine routine,
            LocalDate firstDate,
            LocalDate recurrenceEndDate) {
        if (routine != Flight.ScheduleRoutine.SPECIAL_ADDITIONAL &&
                recurrenceEndDate != null &&
                recurrenceEndDate.isBefore(firstDate)) {
            throw ApiException.badRequest("Schedule end date must be on or after the first departure date");
        }
        if (routine != Flight.ScheduleRoutine.SPECIAL_ADDITIONAL &&
                recurrenceEndDate != null &&
                ChronoUnit.DAYS.between(firstDate, recurrenceEndDate) > 364) {
            throw ApiException.badRequest("A recurring schedule cannot exceed 365 days");
        }
        if (routine == Flight.ScheduleRoutine.ALTERNATE_ODD && firstDate.getDayOfMonth() % 2 == 0 ||
                routine == Flight.ScheduleRoutine.ALTERNATE_EVEN && firstDate.getDayOfMonth() % 2 != 0) {
            throw ApiException.badRequest("The first departure date does not match the selected odd/even routine");
        }
    }

    private void validateRoutineDay(
            Flight.ScheduleRoutine routine,
            Integer weeklyDay,
            Integer monthlyDay) {
        if (routine == Flight.ScheduleRoutine.WEEKLY && (weeklyDay == null || weeklyDay < 1 || weeklyDay > 7)) {
            throw ApiException.badRequest("Choose a weekday from Monday to Sunday for a weekly schedule");
        }
        if (routine == Flight.ScheduleRoutine.MONTHLY && (monthlyDay == null || monthlyDay < 1 || monthlyDay > 31)) {
            throw ApiException.badRequest("Choose a date from 1 to 31 for a monthly schedule");
        }
    }

    private LocalDate nextWeeklyDate(LocalDate currentDate, int weeklyDay) {
        int daysUntilChosenDay = Math.floorMod(weeklyDay - currentDate.getDayOfWeek().getValue(), 7);
        return currentDate.plusDays(daysUntilChosenDay == 0 ? 7 : daysUntilChosenDay);
    }

    private LocalDate nextMonthlyDate(LocalDate currentDate, int monthlyDay) {
        YearMonth month = YearMonth.from(currentDate).plusMonths(1);
        while (monthlyDay > month.lengthOfMonth()) {
            month = month.plusMonths(1);
        }
        return month.atDay(monthlyDay);
    }

    private LocalDate nextAlternateDate(LocalDate currentDate, Flight.ScheduleRoutine routine) {
        LocalDate candidate = currentDate.plusDays(1);
        boolean wantsOdd = routine == Flight.ScheduleRoutine.ALTERNATE_ODD;
        while ((candidate.getDayOfMonth() % 2 == 1) != wantsOdd) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    @Transactional
    public void updateFlightStatus(Long id, String status) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Flight not found: " + id));
        Flight.FlightStatus newStatus = Flight.FlightStatus.valueOf(status.toUpperCase());
        flight.setStatus(newStatus);
        flightRepository.save(flight);

        if (newStatus == Flight.FlightStatus.CANCELLED) {
            cancelConfirmedBookings(flight);
        }
    }

    private void cancelConfirmedBookings(Flight flight) {
        String reason = "Flight cancelled by airline: " + flight.getFlightNumber();
        for (Booking booking : bookingRepository.findConfirmedBookingsByFlightId(flight.getId())) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now());
            booking.setCancellationReason(reason);
            bookingRepository.save(booking);

            auditLogRepository.save(AuditLog.builder()
                    .action(AuditLog.AuditAction.BOOKING_CANCELLED)
                    .entityType("Booking")
                    .entityId(booking.getId())
                    .performedBy("system")
                    .description("Booking cancelled because flight " + flight.getFlightNumber() + " was cancelled")
                    .build());

            paymentRepository.findByBookingId(booking.getId())
                    .filter(payment -> payment.getStatus() == Payment.PaymentStatus.SUCCESS)
                    .ifPresent(payment -> refundService.processAutomaticRefund(
                            booking, payment, reason, "system"));
        }
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 0)
    @Transactional
    public void completeArrivedFlights() {
        int completed = flightRepository.completeArrivedFlights(
                LocalDateTime.now(),
                List.of(Flight.FlightStatus.SCHEDULED, Flight.FlightStatus.DELAYED),
                Flight.FlightStatus.COMPLETED);
        if (completed > 0) {
            log.info("Automatically marked {} arrived flight(s) as completed", completed);
        }
    }

    /**
     * Generates seats from explicit row counts per class.
     *   First Class : rows 1..firstRows,           cols A-D (4 seats/row)
     *   Business    : next businessRows rows,       cols A-F (6 seats/row)
     *   Economy     : next economyRows rows,        cols A-F (6 seats/row)
     */
    private void generateSeatsFromRequest(Flight flight, int firstRows, int businessRows, int economyRows) {
        List<Seat> seats = new ArrayList<>();
        int rowNum = 1;

        // First Class
        for (int r = 0; r < firstRows; r++, rowNum++) {
            for (char col : new char[]{'A', 'B', 'C', 'D'}) {
                seats.add(buildSeat(flight, rowNum, col, Seat.SeatClass.FIRST, BigDecimal.valueOf(6000)));
            }
        }

        // Business
        for (int r = 0; r < businessRows; r++, rowNum++) {
            for (char col : new char[]{'A', 'B', 'C', 'D', 'E', 'F'}) {
                seats.add(buildSeat(flight, rowNum, col, Seat.SeatClass.BUSINESS, BigDecimal.valueOf(3000)));
            }
        }

        // Economy
        for (int r = 0; r < economyRows; r++, rowNum++) {
            for (char col : new char[]{'A', 'B', 'C', 'D', 'E', 'F'}) {
                seats.add(buildSeat(flight, rowNum, col, Seat.SeatClass.ECONOMY, BigDecimal.ZERO));
            }
        }

        seatRepository.saveAll(seats);
        log.info("Generated {} seats for flight {}", seats.size(), flight.getFlightNumber());
    }

    /**
     * Legacy seat generation used by V5 migration (seed flights).
     * Kept public so the migration SQL-driven path still works via the service if needed.
     */
    public void generateSeats(Flight flight) {
        int firstRows    = 2;
        int businessRows = 6;
        int economyRows  = (int) Math.ceil(Math.max(0, flight.getTotalSeats() - 8 - 36) / 6.0);
        generateSeatsFromRequest(flight, firstRows, businessRows, economyRows);
    }

    private Seat buildSeat(Flight flight, int row, char col, Seat.SeatClass seatClass, BigDecimal extraPrice) {
        return Seat.builder()
                .flight(flight)
                .seatNumber(row + String.valueOf(col))
                .seatClass(seatClass)
                .status(Seat.SeatStatus.AVAILABLE)
                .extraPrice(extraPrice)
                .build();
    }

    public FlightResponse toResponse(Flight f) {
        long duration = ChronoUnit.MINUTES.between(f.getDepartureTime(), f.getArrivalTime());
        return FlightResponse.builder()
                .id(f.getId())
                .flightNumber(f.getFlightNumber())
                .airline(FlightResponse.AirlineInfo.builder()
                        .id(f.getAirline().getId())
                        .code(f.getAirline().getCode())
                        .name(f.getAirline().getName())
                        .logoUrl(f.getAirline().getLogoUrl())
                        .build())
                .originAirport(FlightResponse.AirportInfo.builder()
                        .id(f.getOriginAirport().getId())
                        .iataCode(f.getOriginAirport().getIataCode())
                        .name(f.getOriginAirport().getName())
                        .city(f.getOriginAirport().getCity())
                        .country(f.getOriginAirport().getCountry())
                        .build())
                .destinationAirport(FlightResponse.AirportInfo.builder()
                        .id(f.getDestinationAirport().getId())
                        .iataCode(f.getDestinationAirport().getIataCode())
                        .name(f.getDestinationAirport().getName())
                        .city(f.getDestinationAirport().getCity())
                        .country(f.getDestinationAirport().getCountry())
                        .build())
                .departureTime(f.getDepartureTime())
                .arrivalTime(f.getArrivalTime())
                .scheduleRoutine(f.getScheduleRoutine().name())
                .recurrenceEndDate(f.getRecurrenceEndDate())
                .weeklyDay(f.getWeeklyDay())
                .monthlyDay(f.getMonthlyDay())
                .basePrice(f.getBasePrice())
                .totalSeats(f.getTotalSeats())
                .availableSeats(f.getAvailableSeats())
                .status(f.getStatus().name())
                .durationMinutes(duration)
                .build();
    }
}
