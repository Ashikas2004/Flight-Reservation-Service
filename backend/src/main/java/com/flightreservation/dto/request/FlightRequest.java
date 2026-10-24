package com.flightreservation.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FlightRequest {

    @NotBlank(message = "Flight number is required")
    @Size(max = 20)
    private String flightNumber;

    @NotNull(message = "Airline ID is required")
    private Long airlineId;

    @NotNull(message = "Origin airport ID is required")
    private Long originAirportId;

    @NotNull(message = "Destination airport ID is required")
    private Long destinationAirportId;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    private String scheduleRoutine = "SPECIAL_ADDITIONAL";

    private LocalDate recurrenceEndDate;

    private Integer weeklyDay;

    private Integer monthlyDay;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal basePrice;

    /** Number of First Class seats (rows 1-N, cols A-D, 4 per row) */
    @NotNull(message = "First class seats required")
    @Min(value = 0, message = "Cannot be negative")
    private Integer firstClassSeats;

    /** Number of Business class seats (cols A-F, 6 per row) */
    @NotNull(message = "Business seats required")
    @Min(value = 0, message = "Cannot be negative")
    private Integer businessSeats;

    /** Number of Economy class seats (cols A-F, 6 per row) */
    @NotNull(message = "Economy seats required")
    @Min(value = 1, message = "Must have at least 1 economy seat")
    private Integer economySeats;
}
