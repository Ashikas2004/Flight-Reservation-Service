package com.flightreservation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights", indexes = {
        @Index(name = "idx_flight_origin_dest", columnList = "origin_airport_id, destination_airport_id"),
        @Index(name = "idx_flight_departure", columnList = "departure_time"),
        @Index(name = "idx_flight_number", columnList = "flight_number")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_flight_number_departure",
        columnNames = {"flight_number", "departure_time"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, length = 20)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_airport_id", nullable = false)
    private Airport destinationAirport;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_routine", nullable = false, length = 30)
    @Builder.Default
    private ScheduleRoutine scheduleRoutine = ScheduleRoutine.SPECIAL_ADDITIONAL;

    @Column(name = "schedule_series_id", length = 36)
    private String scheduleSeriesId;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @Column(name = "weekly_day")
    private Integer weeklyDay;

    @Column(name = "monthly_day")
    private Integer monthlyDay;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    public enum FlightStatus {
        SCHEDULED, DELAYED, CANCELLED, COMPLETED
    }

    public enum ScheduleRoutine {
        DAILY, WEEKLY, ALTERNATE_ODD, ALTERNATE_EVEN, MONTHLY, SPECIAL_ADDITIONAL
    }
}
