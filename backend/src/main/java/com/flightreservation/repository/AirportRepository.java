package com.flightreservation.repository;

import com.flightreservation.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<Airport, Long> {

    Optional<Airport> findByIataCode(String iataCode);

    boolean existsByIataCode(String iataCode);

    List<Airport> findAllByActiveTrue();

    List<Airport> findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(String city, String name);
}
