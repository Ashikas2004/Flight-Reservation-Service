package com.flightreservation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponse {

    private Long id;
    private String flightNumber;
    private AirlineInfo airline;
    private AirportInfo originAirport;
    private AirportInfo destinationAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String scheduleRoutine;
    private LocalDate recurrenceEndDate;
    private Integer weeklyDay;
    private Integer monthlyDay;
    private BigDecimal basePrice;
    private Integer totalSeats;
    private Integer availableSeats;
    private String status;
    private long durationMinutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirlineInfo {
        private Long id;
        private String code;
        private String name;
        private String logoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AirportInfo {
        private Long id;
        private String iataCode;
        private String name;
        private String city;
        private String country;
    }
}
