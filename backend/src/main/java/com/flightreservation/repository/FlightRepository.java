package com.flightreservation.repository;

import com.flightreservation.entity.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    boolean existsByFlightNumberAndDepartureTime(String flightNumber, LocalDateTime departureTime);

    @Modifying
    @Query("""
            UPDATE Flight f
            SET f.status = :completedStatus
            WHERE f.status IN :eligibleStatuses
              AND f.arrivalTime <= :currentTime
            """)
    int completeArrivedFlights(
            @Param("currentTime") LocalDateTime currentTime,
            @Param("eligibleStatuses") List<Flight.FlightStatus> eligibleStatuses,
            @Param("completedStatus") Flight.FlightStatus completedStatus);

    @Query("""
            SELECT f FROM Flight f
            JOIN FETCH f.airline
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            WHERE f.originAirport.iataCode = :origin
              AND f.destinationAirport.iataCode = :destination
              AND f.departureTime >= :fromTime
              AND f.departureTime < :toTime
              AND f.status = 'SCHEDULED'
              AND f.availableSeats >= :passengerCount
            """)
    Page<Flight> searchFlights(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("passengerCount") int passengerCount,
            Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE f.airline.id = :airlineId")
    List<Flight> findByAirlineId(@Param("airlineId") Long airlineId);

    @Query("""
            SELECT f FROM Flight f
            JOIN FETCH f.airline
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            WHERE f.id = :id
            """)
    Optional<Flight> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT f FROM Flight f
            JOIN FETCH f.airline
            JOIN FETCH f.originAirport
            JOIN FETCH f.destinationAirport
            ORDER BY f.departureTime DESC
            """)
    Page<Flight> findAllWithDetails(Pageable pageable);
}
